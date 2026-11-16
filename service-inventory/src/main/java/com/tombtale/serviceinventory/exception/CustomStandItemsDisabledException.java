package com.tombtale.serviceinventory.exception;

/**
 * A custom stand entry while {@code inventory.custom-stand-items.enabled} is off. Answers 403.
 */
public class CustomStandItemsDisabledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The message names the setting that would allow it. */
    public CustomStandItemsDisabledException() {
        super("Custom stand items are off. inventory.custom-stand-items.enabled turns them on");
    }
}
