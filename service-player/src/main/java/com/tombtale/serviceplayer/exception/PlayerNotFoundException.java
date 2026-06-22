package com.tombtale.serviceplayer.exception;

/**
 * Exception thrown when a requested Player cannot be found in the database.
 */
public class PlayerNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PlayerNotFoundException(Long id) {
        super("Player not found with ID: " + id);
    }
}
