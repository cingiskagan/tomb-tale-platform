package com.tombtale.commons.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * The wire format of every paginated list endpoint on this platform.
 *
 * <p>Controllers return this rather than Spring Data's {@code Page}, whose
 * serialised shape the framework documents as unstable. See ADR 0016.
 *
 * @param content the rows on this page
 * @param page    where this page sits in the whole result
 * @param <T>     the row type
 */
public record PagedResponse<T>(List<T> content, PageInfo page) {

    /**
     * Wraps a Spring Data page in the platform's envelope.
     *
     * @param source the page a service produced
     * @param <T>    the row type
     * @return the same rows and counts, in the published shape
     */
    public static <T> PagedResponse<T> from(Page<T> source) {
        return new PagedResponse<>(
                source.getContent(),
                new PageInfo(
                        source.getNumber(),
                        source.getSize(),
                        source.getTotalElements(),
                        source.getTotalPages()));
    }

    /**
     * Where one page sits in the whole result.
     *
     * @param number        zero-based index of this page
     * @param size          the page size that produced it
     * @param totalElements rows matching the query across every page
     * @param totalPages    number of pages the result is split into
     */
    public record PageInfo(int number, int size, long totalElements, int totalPages) {
    }
}
