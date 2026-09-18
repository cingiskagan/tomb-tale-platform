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
@WebMvcTest(ZitadelEventController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ZitadelEventControllerTest {

    private static final String URL = "/internal/zitadel/user-created";
    private static final String KEY = "test-signing-key";
    private static final String ZITADEL_USER_ID = "zitadel-sub-314159";
    private static final String BODY = "{\"userID\":\"" + ZITADEL_USER_ID + "\"}";

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

        verify(playerService).provisionPlayer(ZITADEL_USER_ID);
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

    @Test
    void bodyChangedAfterSigningIsRefused() throws Exception {
        mockMvc.perform(post(URL)
                .header(ZitadelSignatureVerifier.SIGNATURE_HEADER, signatureFor(BODY, Instant.now(), KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userID\":\"somebody-else\"}"))
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
        String noUser = "{\"somethingElse\":\"x\"}";

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
