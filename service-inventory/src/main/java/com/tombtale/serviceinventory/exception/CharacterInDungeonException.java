package com.tombtale.serviceinventory.exception;

import java.time.Instant;
import java.util.UUID;

/**
 * The character holds a dungeon lock, so nothing crosses the dungeon's border. Answers 409.
 */
public class CharacterInDungeonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param characterId the character
     * @param until       when its lock ends unless the game server renews it
     */
    public CharacterInDungeonException(UUID characterId, Instant until) {
        super("Character " + characterId + " is in a dungeon until " + until);
    }
}
