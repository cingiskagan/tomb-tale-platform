package com.tombtale.serviceinventory.exception;

/**
 * A stand entry names a retired dealer, whose stand shows nothing. Answers 409.
 */
public class RetiredDealerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param code the dealer's code */
    public RetiredDealerException(String code) {
        super("The dealer " + code + " is retired");
    }
}
