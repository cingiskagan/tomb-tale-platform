package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.repository.ItemIconRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.support.PostgresTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Icon uploads and reads from the request to the row, on a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and a matrix of cases needs many methods.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class ItemIconControllerTest extends PostgresTestBase {

    private static final String ICONS_URL = "/api/v1/inventory/icons";
    private static final UUID ADMIN = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");
    private static final int MAX_BYTES = 256 * 1024;
    private static final String PROBLEM_TITLE = "$.title";
    private static final String SWORD_PNG = "sword.png";

    private static final byte[] PNG = bytes((byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, '\r', 'I', 'H');
    private static final byte[] WEBP = bytes('R', 'I', 'F', 'F', 0x1A, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8');

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemIconRepository itemIconRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @AfterEach
    void removeCommittedRows() {
        itemTemplateRepository.deleteAllInBatch();
        itemIconRepository.deleteAllInBatch();
    }

    @Test
    void anUploadedIconIsServedToAnyoneUnderItsHashForAYear() throws Exception {
        upload(SWORD_PNG, PNG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hash").value(sha256(PNG)))
                .andExpect(jsonPath("$.contentType").value("image/png"));

        MockHttpServletResponse icon = mockMvc.perform(get(ICONS_URL + "/" + sha256(PNG)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse();

        assertThat(icon.getContentAsByteArray()).isEqualTo(PNG);
        assertThat(icon.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("max-age=31536000, public, immutable");
        assertThat(icon.getHeaders("X-Content-Type-Options")).containsExactly("nosniff");
    }

    @Test
    void theSameImageTwiceIsStoredOnce() throws Exception {
        upload("first.webp", WEBP).andExpect(jsonPath("$.contentType").value("image/webp"));
        upload("second.webp", WEBP).andExpect(status().isOk());

        assertThat(itemIconRepository.count()).isOne();
    }

    @Test
    void anSvgNamedLikeAPngIsRefused() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        upload(SWORD_PNG, svg)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Unsupported Icon"));
        assertThat(itemIconRepository.count()).isZero();
    }

    @Test
    void aFileOverTheLimitIsRefused() throws Exception {
        byte[] oversized = Arrays.copyOf(PNG, MAX_BYTES + 1);

        upload("huge.png", oversized)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Icon Too Large"));
        assertThat(itemIconRepository.count()).isZero();
    }

    @Test
    void onlyAnAdminUploads() throws Exception {
        mockMvc.perform(multipart(ICONS_URL).file(file(SWORD_PNG, PNG)).with(tokenWithRoles(RoleConstants.GAME_MASTER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart(ICONS_URL).file(file(SWORD_PNG, PNG))).andExpect(status().isUnauthorized());
    }

    @Test
    void anUnknownHashAnswers404() throws Exception {
        mockMvc.perform(get(ICONS_URL + "/" + sha256(PNG)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void aTemplatePointsAtAnUploadedIconButNotAtAnUnknownOne() throws Exception {
        upload(SWORD_PNG, PNG);

        createTemplate("SWORD_IRON", sha256(PNG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iconHash").value(sha256(PNG)));
        createTemplate("SWORD_STEEL", sha256(WEBP))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Unknown Icon"));
    }

    private ResultActions upload(String name, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart(ICONS_URL).file(file(name, bytes))
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)));
    }

    private ResultActions createTemplate(String code, String iconHash) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/templates")
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "code": "%s",
                          "name": "Sword",
                          "itemType": "ONE_HANDED_WEAPON",
                          "price": 150,
                          "baseStats": {},
                          "assetKey": "Items/Weapons/Sword",
                          "iconHash": "%s"
                        }
                        """.formatted(code, iconHash)));
    }

    /** The declared type says PNG whatever the bytes are, as a renamed file's would. */
    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, MediaType.IMAGE_PNG_VALUE, bytes);
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
        return jwt()
                .jwt(token -> token.subject(ADMIN.toString())
                        .claim(PublicIdClaim.CLAIM, ADMIN.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }
}
