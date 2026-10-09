package com.tombtale.commons.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the fixed identities in {@link SystemActor}. */
class SystemActorTest {

    private static final SystemActor ACTOR = SystemActor.COMMERCE_PLAYER_EVENT_CONSUMER;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

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

    @Test
    @DisplayName("the work runs with the actor as its principal")
    void runsAsTheActor() {
        Object principal = ACTOR.run(() -> SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        assertThat(principal).isEqualTo(ACTOR);
    }

    @Test
    @DisplayName("the caller's authentication comes back after the work")
    void restoresTheCallersContext() {
        Authentication caller = new TestingAuthenticationToken("caller", "credentials");
        SecurityContextHolder.getContext().setAuthentication(caller);

        ACTOR.run(() -> null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(caller);
    }

    @Test
    @DisplayName("the caller's authentication comes back when the work throws")
    void restoresTheCallersContextAfterAFailure() {
        Authentication caller = new TestingAuthenticationToken("caller", "credentials");
        SecurityContextHolder.getContext().setAuthentication(caller);

        assertThatThrownBy(() -> ACTOR.run(() -> {
            throw new IllegalStateException("work failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(caller);
    }
}
