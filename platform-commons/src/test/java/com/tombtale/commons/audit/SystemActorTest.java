package com.tombtale.commons.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the fixed identities in {@link SystemActor}. */
class SystemActorTest {

    @Test
    @DisplayName("every actor exposes a parseable id")
    void exposesAnId() {
        assertThat(SystemActor.COMMERCE_PLAYER_EVENT_CONSUMER.actorId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
    }

    @Test
    @DisplayName("no two actors share an id")
    void assignsDistinctIds() {
        assertThat(Arrays.stream(SystemActor.values()).map(SystemActor::actorId).collect(Collectors.toSet()))
                .hasSameSizeAs(SystemActor.values());
    }
}
