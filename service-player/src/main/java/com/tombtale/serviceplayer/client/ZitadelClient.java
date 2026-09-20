package com.tombtale.serviceplayer.client;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceplayer.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

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

    /** How many rows a search asks for at a time. */
    private static final int PAGE_SIZE = 100;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() { };

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
     * to propagate, so the provisioning call fails rather than reporting a
     * success it did not have — a user left with a profile and no role cannot
     * log in at all. Recovering one is
     * {@link com.tombtale.serviceplayer.service.ProvisioningReconciler}'s job.
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

    /**
     * Every human user in the organisation, machine accounts excluded.
     *
     * @return their Zitadel ids
     */
    public List<String> listHumanUserIds() {
        return searchAll("/management/v1/users/_search",
                offset -> Map.of("query", Map.of("offset", offset, "limit", PAGE_SIZE))).stream()
                .filter(user -> user.containsKey("human"))
                .map(user -> (String) user.get("id"))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * The users already holding any role on this project.
     *
     * @return their Zitadel ids
     */
    public List<String> listGrantedUserIds() {
        return searchAll("/management/v1/users/grants/_search",
                offset -> Map.of(
                        "query", Map.of("offset", offset, "limit", PAGE_SIZE),
                        "queries", List.of(Map.of("projectIdQuery", Map.of("projectId", projectId))))).stream()
                .map(grant -> (String) grant.get("userId"))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Who created this account, which is what separates a self-registration
     * from one an administrator made.
     *
     * @param zitadelUserId the account to look up
     * @return the editor of its oldest change, empty if it has none
     */
    public Optional<String> creatorOf(String zitadelUserId) {
        // Oldest change first: for a user aggregate that is user.human.added.
        Map<String, Object> body = restClient.post()
                .uri("/management/v1/users/{userId}/changes/_search", zitadelUserId)
                .body(Map.of("query", Map.of("limit", 1, "asc", true)))
                .retrieve()
                .body(MAP_TYPE);

        return results(body).stream()
                .findFirst()
                .map(change -> (String) change.get("editorId"));
    }

    /**
     * Reads a paged Zitadel search to the end.
     *
     * @param uri              the search endpoint
     * @param requestForOffset builds the body for a given offset
     * @return every row across all pages
     */
    private List<Map<String, Object>> searchAll(String uri, IntFunction<Map<String, Object>> requestForOffset) {
        List<Map<String, Object>> all = new ArrayList<>();
        List<Map<String, Object>> page;

        do {
            Map<String, Object> body = restClient.post()
                    .uri(uri)
                    .body(requestForOffset.apply(all.size()))
                    .retrieve()
                    .body(MAP_TYPE);

            page = results(body);
            all.addAll(page);
            // A short page is the last one; a full page means there may be more.
        } while (page.size() == PAGE_SIZE);

        return all;
    }

    /**
     * @param body a search response, or null
     * @return its result rows, empty when there are none
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> results(Map<String, Object> body) {
        if (body == null || !(body.get("result") instanceof List<?> rows)) {
            return List.of();
        }
        return (List<Map<String, Object>>) rows;
    }
}
