package com.tombtale.servicecommerce.entity;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PurchaseStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @MethodSource("allowedTransitions")
    void allowedTransitionsAreAccepted(PurchaseStatus from, PurchaseStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is blocked")
    @MethodSource("blockedTransitions")
    void blockedTransitionsAreRejected(PurchaseStatus from, PurchaseStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PurchaseStatus.class)
    void selfTransitionsAreNotTableMoves(PurchaseStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @Test
    void cancelledIsTerminal() {
        assertThat(PurchaseStatus.CANCELLED.canTransitionTo(PurchaseStatus.PENDING)).isFalse();
        assertThat(PurchaseStatus.CANCELLED.canTransitionTo(PurchaseStatus.COMPLETED)).isFalse();
        assertThat(PurchaseStatus.CANCELLED.canTransitionTo(PurchaseStatus.REFUNDED)).isFalse();
    }

    @Test
    void everyStatusPairIsClassified() {
        long declared = allowedTransitions().count()
                + blockedTransitions().count()
                + PurchaseStatus.values().length; // the self-pairs from test 3
        int totalPairs = PurchaseStatus.values().length * PurchaseStatus.values().length;

        assertThat(declared).isEqualTo(totalPairs);
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                arguments(PurchaseStatus.PENDING, PurchaseStatus.COMPLETED),
                arguments(PurchaseStatus.PENDING, PurchaseStatus.CANCELLED),
                arguments(PurchaseStatus.COMPLETED, PurchaseStatus.REFUNDED),
                arguments(PurchaseStatus.COMPLETED, PurchaseStatus.CANCELLED),
                arguments(PurchaseStatus.REFUNDED, PurchaseStatus.CANCELLED));
    }

    private static Stream<Arguments> blockedTransitions() {
        return Stream.of(
                arguments(PurchaseStatus.PENDING, PurchaseStatus.REFUNDED),
                arguments(PurchaseStatus.COMPLETED, PurchaseStatus.PENDING),
                arguments(PurchaseStatus.REFUNDED, PurchaseStatus.PENDING),
                arguments(PurchaseStatus.REFUNDED, PurchaseStatus.COMPLETED),
                arguments(PurchaseStatus.CANCELLED, PurchaseStatus.PENDING),
                arguments(PurchaseStatus.CANCELLED, PurchaseStatus.COMPLETED),
                arguments(PurchaseStatus.CANCELLED, PurchaseStatus.REFUNDED));
    }
}
