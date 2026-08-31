package com.tombtale.serviceplayer.repository;

import com.tombtale.serviceplayer.config.JpaConfig;
import com.tombtale.serviceplayer.config.QueryDslConfig;
import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class })
class PlayerQueryRepositoryImplTest extends PostgresTestBase {
    private static final int PAGE_SIZE = 10;
    private static final long THREE_PLAYERS = 3L;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Player aPlayer(String displayName) {
        return Player.builder()
                .publicId(UUID.randomUUID())
                .zitadelUserId("zid" + displayName)
                .displayName(displayName)
                .build();
    }

    private void persist(Player... players) {
        for (Player p : players) {
            entityManager.persist(p);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldFilterByDisplayNameCaseInsensitively() {
        persist(aPlayer("Thorin"), aPlayer("Gimli"));

        Page<Player> page = playerRepository.findByFilter(
                new PlayerFilterRequest("thor"), PageRequest.of(0, PAGE_SIZE));

        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent()).extracting(Player::getDisplayName).containsExactly("Thorin");
    }

    @Test
    void shouldSortByDisplayNameDescending() {
        persist(aPlayer("Arwen"), aPlayer("Bilbo"), aPlayer("Celeborn"));

        Page<Player> page = playerRepository.findByFilter(
                new PlayerFilterRequest(null),
                PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "displayName")));

        assertThat(page.getContent()).extracting(Player::getDisplayName)
                .containsExactly("Celeborn", "Bilbo", "Arwen");
    }

    @Test
    void shouldRejectUnknownSortField() {
        assertThatThrownBy(() -> playerRepository.findByFilter(
                new PlayerFilterRequest(null),
                PageRequest.of(0, PAGE_SIZE, Sort.by("invalidField"))))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field: invalidField");
    }

    @Test
    void shouldCountAllMatchesWhilePagingOne() {
        persist(aPlayer("Arwen"), aPlayer("Bilbo"), aPlayer("Celeborn"));

        Page<Player> page = playerRepository.findByFilter(
                new PlayerFilterRequest(null),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "displayName")));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(THREE_PLAYERS);
    }
}
