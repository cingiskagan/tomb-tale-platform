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
     * The id of the user the event is about.
     *
     * <p>Not {@code userID}, which the same payload also carries. That one is
     * whoever <em>caused</em> the event — the admin who created the account —
     * and provisioning against it would give the admin a second player and the
     * new user none. The aggregate is the thing the event happened to, so for
     * {@code user.human.added} it is the account that was just created, and it
     * is what arrives as the JWT subject later.
     */
    private static final String AGGREGATE_ID_FIELD = "aggregateID";

    /** Guards against provisioning from an event about something other than a user. */
    private static final String AGGREGATE_TYPE_FIELD = "aggregateType";

    /** The only aggregate type this endpoint acts on. */
    private static final String USER_AGGREGATE_TYPE = "user";

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
     * Pulls the created user's id out of the event payload.
     *
     * @param body the raw payload
     * @return the aggregate id, which is the new user's id
     * @throws ResponseStatusException 400 if the body is not JSON, is not about a
     *                                 user, or carries no aggregate id
     */
    private String readUserId(String body) {
        JsonNode root = parse(body);
        requireUserAggregate(root);
        return requireAggregateId(root);
    }

    /**
     * @param body the raw payload
     * @return it as a tree
     * @throws ResponseStatusException 400 if it is not JSON
     */
    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload is not JSON", e);
        }
    }

    /**
     * @param root the parsed payload
     * @throws ResponseStatusException 400 if the event is about anything but a user
     */
    private static void requireUserAggregate(JsonNode root) {
        JsonNode aggregateType = root.get(AGGREGATE_TYPE_FIELD);
        if (aggregateType == null || !USER_AGGREGATE_TYPE.equals(aggregateType.asString())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Not a user event: " + AGGREGATE_TYPE_FIELD + " was " + aggregateType);
        }
    }

    /**
     * @param root the parsed payload
     * @return the created user's id
     * @throws ResponseStatusException 400 if it is missing or not a string
     */
    private static String requireAggregateId(JsonNode root) {
        JsonNode aggregateId = root.get(AGGREGATE_ID_FIELD);
        if (aggregateId == null || !aggregateId.isString() || aggregateId.asString().isBlank()) {
            // Names only, never values: this body describes a real person.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No " + AGGREGATE_ID_FIELD + " in payload. The body carried "
                            + new ArrayList<>(root.propertyNames()));
        }
        return aggregateId.asString();
    }
}
