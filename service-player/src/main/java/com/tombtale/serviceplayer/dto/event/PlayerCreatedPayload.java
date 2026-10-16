package com.tombtale.serviceplayer.dto.event;

import java.util.UUID;

/**
 * The data object of {@code player.created}: a player and their first character.
 * It carries no name, because a name a player picks can identify them (log policy).
 *
 * @param playerPublicId    the new player
 * @param characterPublicId the character created with the player (ADR 0012)
 */
public record PlayerCreatedPayload(
        UUID playerPublicId,
        UUID characterPublicId) {

    /** The event type, which is also the routing key. */
    public static final String EVENT_TYPE = "player.created";

    /** The version of this payload. */
    public static final int EVENT_VERSION = 1;
}
