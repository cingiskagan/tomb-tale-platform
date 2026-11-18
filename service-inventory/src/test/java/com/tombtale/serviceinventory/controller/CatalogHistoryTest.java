package com.tombtale.serviceinventory.controller;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.commons.entity.OutboxEvent;
import com.tombtale.commons.outbox.OutboxEventRepository;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.repository.ItemSetRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.support.PostgresTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every catalog change writes the template or set, as it now stands, into the outbox (ADR 0035).
 * From the token to the row, so each event names the admin who made the change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyStaticImports")
class CatalogHistoryTest extends PostgresTestBase {

    private static final String TEMPLATES_URL = "/api/v1/inventory/templates";
    private static final String SETS_URL = "/api/v1/inventory/sets";
    private static final UUID ADMIN = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");
    private static final int EDITED_DAMAGE = 14;
    private static final int EDITED_ARMOR = 25;

    private static final String SWORD = """
            {"code": "SWORD_IRON", "name": "Iron Sword", "itemType": "ONE_HANDED_WEAPON", "price": 150,
             "baseStats": {"PHYSICAL_DAMAGE": 12}, "assetKey": "Items/Weapons/SwordIron"}""";

    private static final String SHARP_SWORD = """
            {"name": "Sharp Iron Sword", "price": 200, "baseStats": {"PHYSICAL_DAMAGE": %d},
             "assetKey": "Items/Weapons/SwordIron", "version": %%d}""".formatted(EDITED_DAMAGE);

    private static final String IRON_SET = """
            {"code": "IRON_SET", "name": "Iron Set", "bonus": {"MAX_HP": 50}}""";

    private static final String ARMORED_SET = """
            {"name": "Iron Set", "bonus": {"ARMOR": %d}, "version": %%d}""".formatted(EDITED_ARMOR);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private ItemSetRepository itemSetRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void removeCommittedRows() {
        outboxEventRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
        itemSetRepository.deleteAllInBatch();
    }

    /** An edit that changes nothing, a stale edit and a second retire write no event, so each version has one. */
    @Test
    void aTemplatesEventsAreItsVersions() throws Exception {
        String id = idOf(send(post(TEMPLATES_URL), SWORD));
        send(put(TEMPLATES_URL + "/" + id), SHARP_SWORD.formatted(0)).andExpect(status().isOk());
        send(put(TEMPLATES_URL + "/" + id), SHARP_SWORD.formatted(1)).andExpect(status().isOk());
        send(put(TEMPLATES_URL + "/" + id), SHARP_SWORD.formatted(0)).andExpect(status().isConflict());
        send(post(TEMPLATES_URL + "/" + id + "/retire"), "").andExpect(status().isOk());
        send(post(TEMPLATES_URL + "/" + id + "/retire"), "").andExpect(status().isOk());

        List<OutboxEvent> events = eventsAbout(id);
        assertThat(events).extracting(OutboxEvent::getEventType)
                .containsExactly("item-template.created", "item-template.updated", "item-template.retired");
        assertThat(events).extracting(event -> data(event).get("version").asInt()).containsExactly(0, 1, 2);
        assertThat(events).extracting(event -> data(event).get("updatedBy").asString()).containsOnly(ADMIN.toString());

        JsonNode retired = data(events.getLast());
        assertThat(retired.get("name").asString()).isEqualTo("Sharp Iron Sword");
        assertThat(retired.get("baseStats").get("PHYSICAL_DAMAGE").asInt()).isEqualTo(EDITED_DAMAGE);
        assertThat(retired.get("retired").asBoolean()).isTrue();
    }

    @Test
    void aSetsEventsAreItsVersions() throws Exception {
        String id = idOf(send(post(SETS_URL), IRON_SET));
        send(put(SETS_URL + "/" + id), ARMORED_SET.formatted(0)).andExpect(status().isOk());
        send(put(SETS_URL + "/" + id), ARMORED_SET.formatted(1)).andExpect(status().isOk());

        List<OutboxEvent> events = eventsAbout(id);
        assertThat(events).extracting(OutboxEvent::getEventType).containsExactly("item-set.created", "item-set.updated");
        assertThat(events).extracting(event -> data(event).get("version").asInt()).containsExactly(0, 1);
        assertThat(data(events.getLast()).get("bonus").get("ARMOR").asInt()).isEqualTo(EDITED_ARMOR);
        assertThat(data(events.getLast()).get("updatedBy").asString()).isEqualTo(ADMIN.toString());
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request
                .with(jwt()
                        .jwt(token -> token.subject(ADMIN.toString())
                                .claim(PublicIdClaim.CLAIM, ADMIN.toString())
                                .claim(RoleClaimConverter.ROLES_CLAIM, List.of(RoleConstants.PLATFORM_ADMIN)))
                        .authorities(new RoleClaimConverter()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String idOf(ResultActions created) throws Exception {
        return JsonPath.read(created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.publicId");
    }

    private List<OutboxEvent> eventsAbout(String publicId) {
        return outboxEventRepository.findAll(Sort.by("id")).stream()
                .filter(event -> event.getAggregateId().toString().equals(publicId))
                .toList();
    }

    private JsonNode data(OutboxEvent event) {
        return objectMapper.readTree(event.getPayload());
    }
}
