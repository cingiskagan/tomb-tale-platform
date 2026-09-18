package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.security.ZitadelSignatureVerifier;
import com.tombtale.serviceplayer.service.PlayerService;
import com.tombtale.serviceplayer.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives the Zitadel event that a user has been created, and provisions the
 * player row for it. See ADR 0018.
 *
 * <p>Not under {@code /api/v1}, and deliberately. This is a private
 * integration surface rather than published API: Zitadel reaches it on the
 * internal network and Traefik never routes to it from outside.
 *
 * <p>It is also the one endpoint in this service with no {@code @PreAuthorize}.
 * There is no user to authorize — the caller is Zitadel — so
 * {@link ZitadelSignatureVerifier} is what guards it, and the route is
 * permitted in {@code SecurityConfig} for that reason alone.
 */
@Slf4j
@RestController
@RequestMapping("/internal/zitadel")
@RequiredArgsConstructor
public class ZitadelEventController {

    /**
     * Field names the user id might arrive under, in the order we try them.
     *
     * <p>Provisional. Zitadel sends the event as it stores it, and the exact
     * shape is worth pinning to a captured payload once the target has fired
     * for real; until then a rejected call names the fields it did receive.
     */
    private static final List<String> USER_ID_FIELDS = List.of("userID", "userId", "aggregateID");

    private final PlayerService playerService;
    private final ZitadelSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    /**
     * Provisions the player for a newly created Zitadel user.
     *
     * <p>Answers 204 whether it created a row or found one already there.
     * Zitadel retries a call it could not complete, so saying "already done"
     * with an error would turn a retry into a permanent failure.
     *
     * @param signature the request signature, absent on an unsigned call
     * @param body      the raw payload — signed as bytes, so it is read as text
     *                  and parsed here rather than bound to a DTO
     */
    @PostMapping("/user-created")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void userCreated(
            @RequestHeader(name = ZitadelSignatureVerifier.SIGNATURE_HEADER, required = false) String signature,
            @RequestBody String body) {

        if (!signatureVerifier.verify(signature, body)) {
            log.warn("Refused a provisioning call with a missing or invalid signature");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }

        String zitadelUserId = readUserId(body);
        log.info("Provisioning player for Zitadel user: {}", LogUtils.maskId(zitadelUserId));
        playerService.provisionPlayer(zitadelUserId);
    }

    /**
     * Pulls the Zitadel user id out of the event payload.
     *
     * @param body the raw payload
     * @return the user id
     * @throws ResponseStatusException 400 if the body is not JSON, or carries no
     *                                 recognisable user id
     */
    private String readUserId(String body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload is not JSON", e);
        }

        for (String field : USER_ID_FIELDS) {
            JsonNode candidate = root.get(field);
            if (candidate != null && candidate.isString() && !candidate.asString().isBlank()) {
                return candidate.asString();
            }
        }

        // Names only, never values: this body describes a real person.
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No user id in payload. Expected one of " + USER_ID_FIELDS
                        + " but the body carried " + new ArrayList<>(root.propertyNames()));
    }
}
