package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.client.ZitadelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The sweep exists for one case: a self-registered user whose provisioning
 * event never arrived. These pin down who it repairs and, more importantly, who
 * it leaves alone — granting the player role to an account an administrator
 * created would hand out a game role nobody asked for.
 */
@SuppressWarnings("PMD.TooManyStaticImports")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProvisioningReconcilerTest {

    private static final String LOGIN_CLIENT = "391614165389410311";
    private static final String ADMIN = "391614165389344775";

    private static final String SELF_REGISTERED = "100000000000000001";
    private static final String ADMIN_CREATED = "100000000000000002";
    private static final String ALREADY_GRANTED = "100000000000000003";

    @Mock
    private ZitadelClient zitadelClient;

    @Mock
    private PlayerService playerService;

    private ProvisioningReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new ProvisioningReconciler(zitadelClient, playerService, LOGIN_CLIENT);
    }

    @Test
    void repairsSelfRegisteredUserWithNoGrant() {
        when(zitadelClient.listPlayerGrantedUserIds()).thenReturn(List.of());
        when(zitadelClient.listHumanUserIds()).thenReturn(List.of(SELF_REGISTERED));
        when(zitadelClient.creatorOf(SELF_REGISTERED)).thenReturn(Optional.of(LOGIN_CLIENT));

        assertThat(reconciler.reconcile()).isEqualTo(1);
        verify(playerService).provisionPlayer(SELF_REGISTERED);
    }

    /** The whole reason the sweep looks at who created the account. */
    @Test
    void leavesAnAdministratorCreatedUserAlone() {
        when(zitadelClient.listPlayerGrantedUserIds()).thenReturn(List.of());
        when(zitadelClient.listHumanUserIds()).thenReturn(List.of(ADMIN_CREATED));
        when(zitadelClient.creatorOf(ADMIN_CREATED)).thenReturn(Optional.of(ADMIN));

        assertThat(reconciler.reconcile()).isZero();
        verify(playerService, never()).provisionPlayer(anyString());
    }

    /** The normal case: everybody already has their role, so nothing is touched. */
    @Test
    void skipsUsersThatAlreadyHoldTheRole() {
        when(zitadelClient.listPlayerGrantedUserIds()).thenReturn(List.of(ALREADY_GRANTED));
        when(zitadelClient.listHumanUserIds()).thenReturn(List.of(ALREADY_GRANTED));

        assertThat(reconciler.reconcile()).isZero();
        verify(zitadelClient, never()).creatorOf(anyString());
        verify(playerService, never()).provisionPlayer(anyString());
    }

    /** One user's failure is not the next user's problem. */
    @Test
    void carriesOnAfterOneUserFails() {
        when(zitadelClient.listPlayerGrantedUserIds()).thenReturn(List.of());
        when(zitadelClient.listHumanUserIds()).thenReturn(List.of(ADMIN_CREATED, SELF_REGISTERED));
        when(zitadelClient.creatorOf(ADMIN_CREATED)).thenThrow(new ResourceAccessException("Zitadel is down"));
        when(zitadelClient.creatorOf(SELF_REGISTERED)).thenReturn(Optional.of(LOGIN_CLIENT));

        assertThat(reconciler.reconcile()).isEqualTo(1);
        verify(playerService).provisionPlayer(SELF_REGISTERED);
    }

    /**
     * Without the id there is no way to tell the two apart, and guessing would
     * mean granting the role to everybody. Doing nothing is the safe answer.
     */
    @Test
    void doesNothingWhenNoLoginClientIsConfigured() {
        reconciler = new ProvisioningReconciler(zitadelClient, playerService, "  ");

        assertThat(reconciler.reconcile()).isZero();
        verifyNoInteractions(zitadelClient);
        verifyNoInteractions(playerService);
    }

    /** A user with no change history at all is not a self-registration. */
    @Test
    void leavesAUserWithNoCreatorAlone() {
        when(zitadelClient.listPlayerGrantedUserIds()).thenReturn(List.of());
        when(zitadelClient.listHumanUserIds()).thenReturn(List.of(SELF_REGISTERED));
        when(zitadelClient.creatorOf(SELF_REGISTERED)).thenReturn(Optional.empty());

        assertThat(reconciler.reconcile()).isZero();
        verify(playerService, never()).provisionPlayer(anyString());
    }
}
