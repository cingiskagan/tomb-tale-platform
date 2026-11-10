package com.tombtale.serviceinventory.service;

import com.tombtale.commons.web.InvalidSortFieldException;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Refuses a {@code ?sort=} field a list does not allow, so a caller gets a 400 and not a query error.
 */
final class SortAllowList {

    private SortAllowList() {
    }

    /**
     * @param sort    the sort the caller sent
     * @param allowed the entity fields the list sorts by
     * @throws InvalidSortFieldException if the sort names another field
     */
    static void check(Sort sort, Set<String> allowed) {
        for (Sort.Order order : sort) {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidSortFieldException(order.getProperty());
            }
        }
    }
}
