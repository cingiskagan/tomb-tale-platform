package com.tombtale.serviceplayer.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the signature standing in front of the provisioning endpoint.
 *
 * <p>The signing is spelled out here rather than taken from the class under
 * test. A test that reuses the production helper agrees with its bugs.
 */
class ZitadelSignatureVerifierTest {

    private static final String KEY = "test-signing-key";
    private static final String BODY = "{\"userID\":\"zitadel-sub-1\"}";
    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    private final ZitadelSignatureVerifier verifier = new ZitadelSignatureVerifier(KEY, TOLERANCE);

    @Test
    @DisplayName("a body signed with our key, now, is accepted")
    void acceptsAGenuineSignature() {
        assertThat(verifier.verify(signatureFor(BODY, Instant.now(), KEY), BODY)).isTrue();
    }

    @Test
    @DisplayName("a body changed after signing is refused")
    void refusesATamperedBody() {
        String header = signatureFor(BODY, Instant.now(), KEY);

        assertThat(verifier.verify(header, "{\"userID\":\"somebody-else\"}")).isFalse();
    }

    @Test
    @DisplayName("a signature made with another key is refused")
    void refusesAForgedSignature() {
        assertThat(verifier.verify(signatureFor(BODY, Instant.now(), "not-our-key"), BODY)).isFalse();
    }

    @Test
    @DisplayName("a signature older than the tolerance is refused")
    void refusesAReplay() {
        Instant tooOld = Instant.now().minus(TOLERANCE).minusSeconds(1);

        assertThat(verifier.verify(signatureFor(BODY, tooOld, KEY), BODY)).isFalse();
    }

    @Test
    @DisplayName("a missing, malformed or unparseable header is refused")
    void refusesAnythingItCannotRead() {
        assertThat(verifier.verify(null, BODY)).isFalse();
        assertThat(verifier.verify("", BODY)).isFalse();
        assertThat(verifier.verify("v1=deadbeef", BODY)).isFalse();
        assertThat(verifier.verify("t=" + Instant.now().getEpochSecond(), BODY)).isFalse();
        assertThat(verifier.verify("t=not-a-number,v1=deadbeef", BODY)).isFalse();
        assertThat(verifier.verify("t=" + Instant.now().getEpochSecond() + ",v1=nothex", BODY)).isFalse();
    }

    @Test
    @DisplayName("with no key configured nothing is accepted, not even a correct signature")
    void failsClosedWithoutAKey() {
        ZitadelSignatureVerifier unconfigured = new ZitadelSignatureVerifier("  ", TOLERANCE);

        assertThat(unconfigured.verify(signatureFor(BODY, Instant.now(), KEY), BODY)).isFalse();
        assertThat(unconfigured.verify(null, BODY)).isFalse();
    }

    /**
     * Builds the header Zitadel is expected to send: {@code t=<seconds>,v1=<hex>}
     * over {@code <seconds>.<body>}.
     */
    private static String signatureFor(String body, Instant when, String key) {
        long seconds = when.getEpochSecond();
        return "t=" + seconds + ",v1=" + hmacHex(seconds + "." + body, key);
    }

    private static String hmacHex(String signedPayload, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
