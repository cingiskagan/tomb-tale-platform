package com.tombtale.serviceplayer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.tombtale.serviceplayer.config.JpaConfig;
import com.tombtale.serviceplayer.config.QueryDslConfig;
import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.support.PostgresTestBase;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({ QueryDslConfig.class, JpaConfig.class })
class PlayerListQueryCountTest extends PostgresTestBase {

    private static final int PAGE_SIZE = 10;
    private static final long THREE_QUERIES = 3L;
    private static final int ONE_PLAYER = 1;
    private static final int FIVE_PLAYERS = 5;
    private static final int ONE_CHARACTER = 1;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private GameCharacter aCharacter(String name, Player owner) {
        return GameCharacter.builder()
                .publicId(UUID.randomUUID())
                .name(name)
                .player(owner)
                .build();
    }

    private Player aPlayer(String displayName) {
        return Player.builder()
                .publicId(UUID.randomUUID())
                .zitadelUserId("zid" + displayName)
                .displayName(displayName)
                .build();
    }

    private Player aPlayerWithCharacters(String displayName, int characterCount) {
        Player player = aPlayer(displayName);
        for (int i = 0; i < characterCount; i++) {
            player.addCharacter(aCharacter(displayName + "-char" + i, player));
        }
        return player;
    }

    /**
     * Saves the given rows, then flushes and clears the persistence context.
     *
     * <p>
     * The clear matters: without it the repository would answer from
     * Hibernate's first-level cache and the test would never touch Postgres.
     * Clearing forces every later read to go to the database, which is the
     * whole point of running these tests against a real one.
     */
    private void persist(Player... players) {
        for (Player p : players) {
            entityManager.persist(p);
        }
        entityManager.flush();
        entityManager.clear();
    }

    private Statistics resetStatistics() {
        Statistics stats = entityManager.getEntityManager().unwrap(Session.class).getSessionFactory().getStatistics();
        stats.clear();
        return stats;
    }

    @Test
    void shouldKeepQueryCountAtThreeForFivePlayers() {
        persist(aPlayerWithCharacters("Frodo", 1),
                aPlayerWithCharacters("Samwise", 1),
                aPlayerWithCharacters("Peregrin", 1),
                aPlayerWithCharacters("Meriadoc", 1),
                aPlayerWithCharacters("Bilbo", 1));

        Statistics stats = resetStatistics();

        Page<Player> page = playerRepository.findByFilter(
                new PlayerFilterRequest(null), PageRequest.of(0, PAGE_SIZE));

        assertThat(page.getContent()).hasSize(FIVE_PLAYERS);
        assertThat(page.getContent()).allSatisfy(
                p -> assertThat(p.getCharacters()).hasSize(ONE_CHARACTER));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(THREE_QUERIES);
    }

    @Test
    void shouldKeepQueryCountAtThreeForOnePlayer() {
        persist(aPlayerWithCharacters("Eru", 1));

        Statistics stats = resetStatistics();

        Page<Player> page = playerRepository.findByFilter(
                new PlayerFilterRequest(null), PageRequest.of(0, PAGE_SIZE));

        assertThat(page.getContent()).hasSize(ONE_PLAYER);
        assertThat(page.getContent()).allSatisfy(
                p -> assertThat(p.getCharacters()).hasSize(ONE_CHARACTER));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(THREE_QUERIES);
    }
}
