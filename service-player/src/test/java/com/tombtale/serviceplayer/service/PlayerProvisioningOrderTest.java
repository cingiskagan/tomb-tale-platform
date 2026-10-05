package com.tombtale.serviceplayer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import com.tombtale.serviceplayer.client.ZitadelClient;
import com.tombtale.serviceplayer.config.JpaConfig;
import com.tombtale.serviceplayer.config.QueryDslConfig;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.mapper.PlayerMapperImpl;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.serviceplayer.support.PostgresTestBase;

/**
 * Proves the provisioning order from ADR 0023: no player row commits without
 * its publicId metadata, and the role is granted only after the commit.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class, PlayerService.class, PlayerMapperImpl.class, OutboxService.class })
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@SuppressWarnings({ "PMD.TooManyStaticImports" })
class PlayerProvisioningOrderTest extends PostgresTestBase {

    @MockitoBean
    private ZitadelClient zitadelClient;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private String subject;

    @BeforeEach
    void freshSubject() {
        subject = "order-" + UUID.randomUUID();
    }

    /**
     * Deletes the player's outbox rows, then the player. These writes commit,
     * and the container is shared with classes that count rows.
     */
    @AfterEach
    void removeCommittedRows() {
        playerRepository.findByZitadelUserIdWithCharacters(subject).ifPresent(player -> {
            outboxEventRepository.deleteAll(outboxEventRepository.findAll().stream()
                    .filter(event -> event.getAggregateId().equals(player.getPublicId()))
                    .toList());
            playerRepository.delete(player);
        });
    }

    /**
     * The control: the row commits, and the metadata write comes before the
     * grant. Without a committed row here, the empty table below proves nothing.
     */
    @Test
    void writesMetadataBeforeGrantingTheRole() {
        playerService.provisionPlayer(subject);

        Optional<Player> player = playerRepository.findByZitadelUserIdWithCharacters(subject);

        assertThat(player).isPresent();

        InOrder order = inOrder(zitadelClient);
        order.verify(zitadelClient).writePublicId(subject, player.get().getPublicId());
        order.verify(zitadelClient).grantPlayerRole(subject);
    }

    /**
     * A failed metadata write rolls the player back, so the role is never
     * granted.
     */
    @Test
    void failedMetadataWriteCommitsNoRowAndGrantsNoRole() {
        doThrow(new ResourceAccessException("down")).when(zitadelClient).writePublicId(eq(subject), any());
        long outboxCount = outboxEventRepository.count();

        assertThatThrownBy(() -> playerService.provisionPlayer(subject))
                .isInstanceOf(ResourceAccessException.class);

        assertThat(playerRepository.findByZitadelUserIdWithCharacters(subject)).isEmpty();
        assertThat(outboxEventRepository.count()).isEqualTo(outboxCount);
        verify(zitadelClient, never()).grantPlayerRole(anyString());
    }
}
