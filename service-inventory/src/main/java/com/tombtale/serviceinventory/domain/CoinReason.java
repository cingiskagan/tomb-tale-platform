package com.tombtale.serviceinventory.domain;

/**
 * Why a character's coins changed. Every coin history row names one.
 */
public enum CoinReason {

    /** The coins every new character starts with. */
    STARTING_COINS,

    /** The price of an item bought from a dealer, under the buy's trade id. */
    DEALER_BUY
}
