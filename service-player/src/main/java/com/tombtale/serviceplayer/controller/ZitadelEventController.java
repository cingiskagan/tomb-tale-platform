package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.security.ZitadelSignatureVerifier;
import com.tombtale.serviceplayer.service.PlayerService;
import com.tombtale.serviceplayer.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * The id of whoever caused the event, which is not the account it is about.
     *
     * <p>Login V2 does not let a user create their own account — it calls the
     * API as the login client on their behalf — so a self-registration carries
     * that machine user's id here. An account made in the console carries the
     * administrator's, and one made by a service account carries its own.
     * Zitadel raises {@code user.human.added} for all three and has no separate
     * self-registration event, so this field is the only thing that tells them
     * apart.
     */
    private static final String ACTING_USER_ID_FIELD = "userID";

    private final PlayerService playerService;
    private final ZitadelSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    /** The login client's id, or blank when nothing has written it back yet. */
    private final String loginClientId;

    /**
     * @param playerService      creates the player row
     * @param signatureVerifier  what stands in for authentication here
     * @param objectMapper       parses the raw body
     * @param loginClientId      the machine user a self-registration is created
     *                           by; blank provisions every new account
     */
    public ZitadelEventController(
            PlayerService playerService,
            ZitadelSignatureVerifier signatureVerifier,
            ObjectMapper objectMapper,
            @Value("${app.zitadel.registration.login-client-id:}") String loginClientId) {
        this.playerService = playerService;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.loginClientId = loginClientId == null ? "" : loginClientId.trim();
    }

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

        // The payload is validated before it is filtered, so a body we cannot
        // read is still a 400 rather than a quiet 204 that hides the real shape.
        JsonNode root = parse(body);
        requireUserAggregate(root);
        String zitadelUserId = requireAggregateId(root);

        if (!selfRegistered(root)) {
            log.info("Not provisioning: the account was created by somebody other than the login client");
            return;
        }

        log.info("Provisioning player for Zitadel user: {}", LogUtils.maskId(zitadelUserId));
        playerService.provisionPlayer(zitadelUserId);
    }

    /**
     * Whether this account was created by somebody signing themselves up.
     *
     * <p>A missing id is not a self-registration: Zitadel leaves it empty for
     * the accounts it creates itself at instance init, and those are nobody's
     * player.
     *
     * @param root the parsed payload
     * @return whether the player role should be granted
     */
    private boolean selfRegistered(JsonNode root) {
        if (loginClientId.isEmpty()) {
            log.warn("app.zitadel.registration.login-client-id is not set, so every new account is "
                    + "provisioned, including ones an administrator created. Re-run zitadel-setup.sh.");
            return true;
        }

        JsonNode actor = root.get(ACTING_USER_ID_FIELD);
        return actor != null && actor.isString() && loginClientId.equals(actor.asString());
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
