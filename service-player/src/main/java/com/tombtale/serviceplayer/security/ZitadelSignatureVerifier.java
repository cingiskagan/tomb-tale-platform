package com.tombtale.serviceplayer.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Checks that a request really came from Zitadel.
 *
 * <p>The provisioning endpoint carries no user token — Zitadel calls it, not a
 * person — so this signature is the only thing standing in front of it. It
 * fails closed: no key configured means nothing is accepted.
 *
 * <p>The header is {@code t=<unix seconds>,v1=<hex>}, where the hex is
 * HMAC-SHA256 over {@code <t>.<body>} keyed with the target's signing key.
 * Confirm that against the Zitadel release you registered the target on; if it
 * differs, this class is the one place that has to change.
 */
public class ZitadelSignatureVerifier {

    /** Header Zitadel puts the signature in. */
    public static final String SIGNATURE_HEADER = "ZITADEL-Signature";

    private static final Logger LOG = LoggerFactory.getLogger(ZitadelSignatureVerifier.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TIMESTAMP_PREFIX = "t=";
    private static final String SIGNATURE_PREFIX = "v1=";
    private static final String SIGNED_PAYLOAD_SEPARATOR = ".";

    private final byte[] signingKey;
    private final Duration tolerance;

    /**
     * @param signingKey the target's signing key; blank disables the endpoint
     * @param tolerance  how old a signed timestamp may be before it is refused
     */
    public ZitadelSignatureVerifier(String signingKey, Duration tolerance) {
        this.signingKey = signingKey == null || signingKey.isBlank()
                ? new byte[0]
                : signingKey.getBytes(StandardCharsets.UTF_8);
        this.tolerance = tolerance;
    }

    /**
     * Decides whether this body was signed with our key, recently.
     *
     * @param signatureHeader the {@code ZITADEL-Signature} header, may be null
     * @param body            the exact bytes of the request body, as text
     * @return true only if the signature matches and the timestamp is fresh
     */
    public boolean verify(String signatureHeader, String body) {
        if (signingKey.length == 0) {
            LOG.error("No Zitadel webhook signing key is configured, so every provisioning call is refused. "
                    + "Set app.zitadel.webhook.signing-key to the key shown when the target was created.");
            return false;
        }
        if (signatureHeader == null || body == null) {
            return false;
        }

        SignedHeader header = SignedHeader.parse(signatureHeader);
        if (header == null || !isFresh(header.timestamp())) {
            return false;
        }

        return matches(header, body);
    }

    /**
     * The two values the header carries, once they have been picked out of it.
     *
     * @param timestamp the {@code t=} value, unix seconds, still unparsed
     * @param signature the {@code v1=} value, still hex
     */
    private record SignedHeader(String timestamp, String signature) {

        /**
         * @param raw the whole header value
         * @return its two parts, or null if either is missing
         */
        static SignedHeader parse(String raw) {
            String timestamp = null;
            String signature = null;

            for (String part : raw.split(",")) {
                String element = part.trim();
                if (element.startsWith(TIMESTAMP_PREFIX)) {
                    timestamp = element.substring(TIMESTAMP_PREFIX.length());
                } else if (element.startsWith(SIGNATURE_PREFIX)) {
                    signature = element.substring(SIGNATURE_PREFIX.length());
                }
            }

            return timestamp == null || signature == null ? null : new SignedHeader(timestamp, signature);
        }
    }

    /**
     * @param header the parsed header
     * @param body   the request body
     * @return true if the signature is ours, compared in constant time
     */
    private boolean matches(SignedHeader header, String body) {
        byte[] given;
        try {
            given = HexFormat.of().parseHex(header.signature());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return MessageDigest.isEqual(sign(header.timestamp() + SIGNED_PAYLOAD_SEPARATOR + body), given);
    }

    /**
     * Rejects a signature old enough to be a replay.
     *
     * @param timestamp the {@code t=} value, unix seconds
     * @return true if it is within tolerance of now
     */
    private boolean isFresh(String timestamp) {
        try {
            Duration age = Duration.between(Instant.ofEpochSecond(Long.parseLong(timestamp)), Instant.now());
            return age.abs().compareTo(tolerance) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param signedPayload the string Zitadel signed
     * @return its HMAC under our key
     */
    private byte[] sign(String signedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", e);
        }
    }
}
