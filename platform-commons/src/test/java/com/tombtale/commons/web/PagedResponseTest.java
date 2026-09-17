package com.tombtale.commons.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the paged envelope.
 *
 * <p>The serialisation test is the point of the class. The portal mirrors this
 * shape by hand, so a rename here has to fail a build rather than a page.
 */
class PagedResponseTest {

    private static final int PAGE_SIZE = 2;
    private static final long TOTAL_ELEMENTS = 5L;
    private static final int TOTAL_PAGES = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("from() carries the rows and the counts of the source page")
    void copiesRowsAndCounts() {
        Page<String> source = new PageImpl<>(
                List.of("second", "third"), PageRequest.of(1, PAGE_SIZE), TOTAL_ELEMENTS);

        PagedResponse<String> response = PagedResponse.from(source);

        assertThat(response.content()).containsExactly("second", "third");
        assertThat(response.page()).isEqualTo(
                new PagedResponse.PageInfo(1, PAGE_SIZE, TOTAL_ELEMENTS, TOTAL_PAGES));
    }

    @Test
    @DisplayName("from() keeps the requested page size on an empty result")
    void keepsPageSizeWhenEmpty() {
        PagedResponse<String> response = PagedResponse.from(
                new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0L));

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(new PagedResponse.PageInfo(0, PAGE_SIZE, 0L, 0));
    }

    @Test
    @DisplayName("the JSON carries exactly content and page, and page exactly four counts")
    void serialisesToThePublishedShape() {
        String json = objectMapper.writeValueAsString(PagedResponse.from(
                new PageImpl<>(List.of("only"), PageRequest.of(0, PAGE_SIZE), 1L)));

        JsonNode root = objectMapper.readTree(json);

        assertThat(root.properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("content", "page");
        assertThat(root.get("page").properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("number", "size", "totalElements", "totalPages");
        assertThat(root.get("content").get(0).asString()).isEqualTo("only");
        assertThat(root.get("page").get("totalElements").asLong()).isEqualTo(1L);
    }
}
