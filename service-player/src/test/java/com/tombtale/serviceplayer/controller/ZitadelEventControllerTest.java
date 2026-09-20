package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.config.SecurityConfig;
import com.tombtale.serviceplayer.security.ZitadelSignatureVerifier;
import com.tombtale.serviceplayer.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for the provisioning endpoint.
 *
 * <p>This is the one route in the service with no token behind it, so these
 * tests are really about one question: can anybody who can reach the port make
 * us create a player? The signing key comes from {@code application-test.yml},
 * through the same {@code SecurityConfig} bean production uses.
 */
// TooManyStaticImports: MockMvc's fluent API is assembled from static imports,
// and the Mockito verifications are what prove the wrong user is not provisioned.
// TooManyMethods: this endpoint has no token behind it, so every way in is a
// case worth its own test. Splitting them to satisfy a count would hide that.
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.TooManyMethods"})
@WebMvcTest(ZitadelEventController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ZitadelEventControllerTest {

    private static final String URL = "/internal/zitadel/user-created";
    private static final String KEY = "test-signing-key";

    /** The account that was created — the aggregate the event is about. */
    private static final String CREATED_USER_ID = "336494809936035843";

    /**
     * Who created it — the login client, which is what a self-registration looks
     * like. Carried by the same payload, and not who we provision.
     * Matches {@code app.zitadel.registration.login-client-id} in the test profile.
     */
    private static final String ACTING_USER_ID = "336392597046755331";

    /** Any other actor: an administrator in the console, or a service account. */
    private static final String ADMIN_USER_ID = "336392597046000001";

    /** Shaped after the payload in Zitadel's own Actions v2 event documentation. */
    private static final String BODY = """
            {
              "aggregateID": "336494809936035843",
              "aggregateType": "user",
              "resourceOwner": "336392597046099971",
              "instanceID": "336392597046034435",
              "version": "v2",
              "sequence": 1,
              "event_type": "user.human.added",
              "created_at": "2026-09-05T08:55:36.156333Z",
              "userID": "336392597046755331",
              "event_payload": {
                "email": "mini@mouse.com",
                "displayName": "Minnie Mouse"
              }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void signedCallProvisionsThePlayer() throws Exception {
        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(BODY, Instant.now(), KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isNoContent());

        verify(playerService).provisionPlayer(CREATED_USER_ID);
    }

    @Test
    void unsignedCallIsRefused() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerService);
    }

    @Test
    void callSignedWithTheWrongKeyIsRefused() throws Exception {
        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(BODY, Instant.now(), "forged"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerService);
    }

    /**
     * The payload carries two user ids. Provisioning the wrong one would give the
     * admin who created the account a second player and the new user none — and
     * it would pass unnoticed wherever a user signs themselves up.
     */
    @Test
    void provisionsTheCreatedUserRatherThanTheOneWhoCreatedThem() throws Exception {
        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(BODY, Instant.now(), KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isNoContent());

        verify(playerService).provisionPlayer(CREATED_USER_ID);
        verify(playerService, never()).provisionPlayer(ACTING_USER_ID);
    }

    /**
     * The point of the whole filter. Zitadel raises the same
     * {@code user.human.added} whoever created the account, so without this an
     * administrator adding a colleague in the console would hand them a player
     * and a game role they never asked for.
     *
     * <p>Answered 204, not an error: the event is legitimate and Zitadel retries
     * anything else, which would turn "nothing to do" into a permanent failure.
     */
    @Test
    void accountCreatedByAnAdministratorIsNotProvisioned() throws Exception {
        String adminCreated = BODY.replace(ACTING_USER_ID, ADMIN_USER_ID);

        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(adminCreated, Instant.now(), KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(adminCreated))
                .andExpect(status().isNoContent());

        verifyNoInteractions(playerService);
    }

    /**
     * Zitadel leaves the actor empty for the accounts it creates itself at
     * instance init. Those are nobody's player either.
     */
    @Test
    void accountWithNoActorIsNotProvisioned() throws Exception {
        String noActor = BODY.replace("\"userID\": \"" + ACTING_USER_ID + "\",", "");

        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(noActor, Instant.now(), KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(noActor))
                .andExpect(status().isNoContent());

        verifyNoInteractions(playerService);
    }

    /** An event about something that is not a user must not create a player. */
    @Test
    void eventAboutAnotherAggregateIsRefused() throws Exception {
        String orgEvent = "{\"aggregateID\":\"1\",\"aggregateType\":\"org\"}";

        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(orgEvent, Instant.now(), KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orgEvent))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(playerService);
    }

    @Test
    void bodyChangedAfterSigningIsRefused() throws Exception {
        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(BODY, Instant.now(), KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aggregateID\":\"somebody-else\",\"aggregateType\":\"user\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerService);
    }

    @Test
    void staleSignatureIsRefused() throws Exception {
        Instant tooOld = Instant.now().minus(Duration.ofHours(1));

        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(BODY, tooOld, KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(playerService);
    }

    /**
     * A signed payload we cannot find a user in is a 400, not a 500, and the
     * reason names the fields it did carry so the real shape can be pinned.
     */
    @Test
    void signedPayloadWithoutAUserIdIsABadRequest() throws Exception {
        String noUser = "{\"aggregateType\":\"user\",\"somethingElse\":\"x\"}";

        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(noUser, Instant.now(), KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(noUser))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(playerService);
    }

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
