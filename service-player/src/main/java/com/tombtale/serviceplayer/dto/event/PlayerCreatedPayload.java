package com.tombtale.serviceplayer.dto.event;

import java.util.UUID;

/**
 * The data object of {@code player.created}: a player and their first character.
 * Identifiers are public ones, because the Zitadel subject stays here (ADR 0014).
 *
 * @param playerPublicId    the new player
 * @param displayName       the generated name, so a consumer can label the fact
 * @param characterPublicId the character created with the player (ADR 0012)
 */
public record PlayerCreatedPayload(
        UUID playerPublicId,
        String displayName,
        UUID characterPublicId) {

    /** The event type, which is also the routing key. */
    public static final String EVENT_TYPE = "player.created";

    /** The version of this payload. */
    public static final int EVENT_VERSION = 1;
}
