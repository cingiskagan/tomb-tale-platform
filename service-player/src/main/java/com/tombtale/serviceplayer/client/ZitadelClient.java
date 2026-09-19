package com.tombtale.serviceplayer.client;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceplayer.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * The only outbound call this service makes to Zitadel.
 *
 * <p>A self-registered user arrives with no role, and the project is created
 * with {@code projectRoleCheck}, so Zitadel will not issue them a token until
 * something grants them one. Nothing inside Zitadel can do it on v4: Actions v1
 * had {@code appendUserGrant} for exactly this and no longer fires, and a v2
 * function runs too late, after the check that would have refused the token. So
 * the grant is made from here.
 *
 * <p>Uses the management API rather than {@code AuthorizationService/CreateAuthorization},
 * because this endpoint is the one verified against a running instance.
 */
@Slf4j
@Component
public class ZitadelClient {

    private final RestClient restClient;
    private final String projectId;

    /**
     * @param restClient the Zitadel-bound client, carrying the service token
     * @param projectId  the project the role belongs to
     */
    public ZitadelClient(
            @Qualifier("zitadelRestClient") RestClient restClient,
            @Value("${app.zitadel.api.project-id:}") String projectId) {
        this.restClient = restClient;
        this.projectId = projectId;
    }

    /**
     * Grants the {@code player} role so the user can obtain a token at all.
     *
     * <p>Safe to call for a user who already has it: Zitadel answers 409, which
     * is the outcome we wanted rather than a failure. Anything else is allowed
     * to propagate, so the provisioning call fails and Zitadel retries it — a
     * user left with a profile and no role could not log in, and silence here
     * would make that look like a working registration.
     *
     * @param zitadelUserId the subject of the newly registered user
     */
    public void grantPlayerRole(String zitadelUserId) {
        if (projectId.isBlank()) {
            throw new IllegalStateException(
                    "app.zitadel.api.project-id is not set, so the player role cannot be granted. "
                            + "A self-registered user would be left unable to log in. "
                            + "zitadel-setup.sh writes this value.");
        }

        try {
            restClient.post()
                    .uri("/management/v1/users/{userId}/grants", zitadelUserId)
                    .body(Map.of("projectId", projectId, "roleKeys", List.of(RoleConstants.PLAYER)))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Granted {} to Zitadel user: {}", RoleConstants.PLAYER, LogUtils.maskId(zitadelUserId));
        } catch (HttpClientErrorException.Conflict e) {
            log.debug("Zitadel user {} already holds {}", LogUtils.maskId(zitadelUserId), RoleConstants.PLAYER);
        }
    }
}
