package com.tombtale.serviceplayer.exception;

/**
 * Exception thrown when a requested Player cannot be found in the database.
 */
public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(Long id) {
        super("Player not found with ID: " + id);
    }
}
