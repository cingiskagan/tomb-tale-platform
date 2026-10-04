package com.tombtale.commons.security;

/**
 * Where an access token carries the player's {@code publicId} (ADR 0023). service-player writes
 * it as Zitadel user metadata, and Zitadel copies every key into {@link #CLAIM}, base64-encoded.
 */
public final class PublicIdClaim {

    /** The claim, present only when the client requests the scope of the same name. */
    public static final String CLAIM = "urn:zitadel:iam:user:metadata";

    /** The metadata key that holds the {@code publicId}. */
    public static final String KEY = "publicId";

    private PublicIdClaim() {
        // Constants holder.
    }
}
