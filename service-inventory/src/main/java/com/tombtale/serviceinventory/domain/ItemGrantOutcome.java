package com.tombtale.serviceinventory.domain;

/**
 * Whether an item grant added the items or refused them. Each outcome goes out as its own event type.
 */
public enum ItemGrantOutcome {

    /** The items are in the player's stacks. */
    GRANTED("item.granted"),

    /** Inventory refused the items, for the reason the item grant records. */
    REJECTED("item.grant-rejected");

    private final String type;

    ItemGrantOutcome(String type) {
        this.type = type;
    }

    /**
     * The event that carries an item grant with this outcome.
     *
     * @return the event type, which is also the routing key
     */
    public String eventType() {
        return type;
    }
}
