package com.tombtale.commons.audit;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Identities for writers that are not people.
 *
 * <p>A row's {@code createdBy} normally holds a player's {@code publicId}.
 * Some rows have no human behind them — an event consumer, a scheduled job —
 * and those write as one of these instead of leaving the column null.
 *
 * <p>The values are hardcoded, so an audit row means the same thing in every
 * environment; ids generated per environment would not compare. They are
 * zero-padded so that a UUID belonging to a service is obvious next to the
 * random one belonging to a player.
 */
public enum SystemActor {

    /** service-commerce applying a player event it consumed from the broker. */
    COMMERCE_PLAYER_EVENT_CONSUMER("00000000-0000-0000-0000-0000000000c1"),

    /** service-player marking its outbox rows published once the broker confirms them. */
    PLAYER_OUTBOX_PUBLISHER("00000000-0000-0000-0000-0000000000b1");

    private final UUID id;

    SystemActor(String id) {
        this.id = UUID.fromString(id);
    }

    /**
     * The fixed identifier this actor writes as.
     *
     * @return the actor's UUID
     */
    public UUID actorId() {
        return id;
    }

    /**
     * Runs the work as this actor, so every row it writes names this actor, then restores the caller's context.
     * Start the work's transaction inside it: Hibernate asks for the author when the transaction commits.
     *
     * @param work the writes to run
     * @param <T>  what the work returns
     * @return what the work returned
     */
    public <T> T run(Supplier<T> work) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(this, null, List.of()));
        SecurityContextHolder.setContext(context);

        try {
            return work.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
