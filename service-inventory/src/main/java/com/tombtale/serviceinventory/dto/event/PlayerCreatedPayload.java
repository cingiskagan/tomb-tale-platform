package com.tombtale.serviceinventory.dto.event;

import java.util.Objects;
import java.util.UUID;

/**
 * The data of {@code player.created}, which service-player sends with every new player and their first character.
 *
 * @param playerPublicId    the new player, who owns the character
 * @param characterPublicId the character, whose inventory inventory creates
 */
public record PlayerCreatedPayload(UUID playerPublicId, UUID characterPublicId) {

    /** The event type, which is also the routing key. */
    public static final String EVENT_TYPE = "player.created";

    /** An event that misses a field fails conversion, so it goes to the DLQ with no retry. */
    public PlayerCreatedPayload {
        Objects.requireNonNull(playerPublicId, "playerPublicId");
        Objects.requireNonNull(characterPublicId, "characterPublicId");
    }
}
