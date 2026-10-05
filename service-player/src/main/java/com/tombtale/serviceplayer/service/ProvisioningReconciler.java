package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.client.ZitadelClient;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.serviceplayer.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Grants the player role to self-registered users the provisioning event never
 * reached, and gives a player row to role holders who have none.
 *
 * <p>The event is the fast path and this is the backstop. A service that was
 * down when the event fired never receives it: Zitadel cancels the queued call
 * on its first failure rather than retrying, measured against a stopped
 * service-player. Nothing else recovers that.
 * The role is what {@code projectRoleCheck} demands before it will issue a
 * token, so the user cannot reach this service at all and never triggers the
 * fallback in {@link PlayerService}. They are locked out for good, and the
 * login only says "Unknown error occurred", because Zitadel ships no English
 * text for the error it actually hit.
 *
 * <p>A role granted by hand skips the event, because the event provisions only
 * self-registrations. Without a row, that player has no {@code publicId} in their token (ADR 0023).
 *
 * <p>Runs in every instance rather than electing a leader. The work is
 * idempotent — a grant that is already there is left as it is, and a row that
 * exists is recovered rather than duplicated — so a duplicate sweep costs a
 * few wasted calls and nothing else.
 */
@Slf4j
@Component
public class ProvisioningReconciler {

    private final ZitadelClient zitadelClient;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final String loginClientId;

    /**
     * @param zitadelClient     reads users and grants, and makes the grant
     * @param playerService     creates the player row alongside it
     * @param playerRepository  tells which users have a row already
     * @param loginClientId     the machine user a self-registration is created by;
     *                          blank disables the sweep
     */
    public ProvisioningReconciler(
            ZitadelClient zitadelClient,
            PlayerService playerService,
            PlayerRepository playerRepository,
            @Value("${app.zitadel.registration.login-client-id:}") String loginClientId) {
        this.zitadelClient = zitadelClient;
        this.playerService = playerService;
        this.playerRepository = playerRepository;
        this.loginClientId = loginClientId == null ? "" : loginClientId.trim();
    }

    /**
     * Provisions self-registered users without the player role on this project,
     * and holders of the role without a player row.
     *
     * @return how many were repaired
     */
    @Scheduled(
            initialDelayString = "${app.zitadel.registration.reconcile-initial-delay:PT2M}",
            fixedDelayString = "${app.zitadel.registration.reconcile-interval:PT15M}")
    public int reconcile() {
        if (loginClientId.isEmpty()) {
            log.debug("No login client id configured, so the provisioning sweep cannot tell a "
                    + "self-registration from an account an administrator created. Skipping.");
            return 0;
        }

        Set<String> granted = new HashSet<>(zitadelClient.listPlayerGrantedUserIds());
        Set<String> withRow = new HashSet<>(playerRepository.findAllZitadelUserIds());
        List<String> humans = zitadelClient.listHumanUserIds();

        int repaired = 0;
        for (String userId : humans) {
            boolean holdsRole = granted.contains(userId);
            if (holdsRole && withRow.contains(userId)) {
                continue;
            }
            if (repair(userId, holdsRole)) {
                repaired++;
            }
        }
        return repaired;
    }

    /**
     * Provisions one user, if they hold the role or registered themselves.
     *
     * <p>Only the failures that are somebody else's problem are swallowed — a
     * call that did not reach Zitadel, a database that refused the write. The
     * next user is a different lockout and the sweep runs again anyway.
     * Anything else, a blank project id being the one that matters, is left to
     * propagate: that is broken configuration and repeating it per user would
     * only bury it.
     *
     * @param zitadelUserId the account to repair
     * @param holdsRole     whether it holds the player role, which makes its creator irrelevant
     * @return whether it was repaired
     */
    private boolean repair(String zitadelUserId, boolean holdsRole) {
        try {
            if (holdsRole) {
                log.info("Provisioning Zitadel user {}, who holds the player role but has no player row",
                        LogUtils.maskId(zitadelUserId));
            } else if (loginClientId.equals(zitadelClient.creatorOf(zitadelUserId).orElse(null))) {
                log.warn("Provisioning missed: repairing self-registered user {}. "
                        + "Check the Zitadel target and execution.", LogUtils.maskId(zitadelUserId));
            } else {
                return false;
            }

            playerService.provisionPlayer(zitadelUserId);
            return true;
        } catch (RestClientException | DataAccessException e) {
            log.error("Could not repair Zitadel user {}, leaving it for the next sweep",
                    LogUtils.maskId(zitadelUserId), e);
            return false;
        }
    }
}
