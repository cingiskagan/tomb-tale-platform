package com.tombtale.commons.audit;

import java.util.UUID;

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
    COMMERCE_PLAYER_EVENT_CONSUMER("00000000-0000-0000-0000-0000000000c1");

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
}
