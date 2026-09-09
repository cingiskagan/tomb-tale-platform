package com.tombtale.serviceplayer.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tombtale.serviceplayer.config.JpaConfig;
import com.tombtale.serviceplayer.config.QueryDslConfig;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.support.PostgresTestBase;

/**
 * Proves that a player and their first character are written all or nothing.
 *
 * <p>This is the claim {@code PlayerService.getOrCreatePlayer} rests on. It used
 * to be covered by {@code backfillCharacterIfMissing}, which gave any
 * characterless player a character on their next login — a repair pass that hid
 * the question rather than answering it. With the backfill gone, the guarantee
 * comes from the mapping alone: {@code Player.characters} cascades, so one
 * {@code save} is one transaction, and either both rows land or neither does.
 *
 * <p>Note what the failing case actually exercises. {@code Player.id} is an
 * IDENTITY column, so Hibernate must insert the player row at {@code persist}
 * time to learn its key. The row therefore exists inside the transaction before
 * the character insert is even attempted. The test asserts it is gone
 * afterwards, which is the rollback doing the work rather than lucky ordering.
 *
 * <p>The class-level {@code NOT_SUPPORTED} turns off the transaction that
 * {@code @DataJpaTest} would normally wrap each test in and roll back. That is
 * required twice over: a flush failure inside the test's own transaction would
 * poison the persistence context before anything could be asserted, and
 * production runs this path with no ambient transaction either
 * ({@code getOrCreatePlayer} is {@code NOT_SUPPORTED}). The writes here are real
 * commits, so {@link #removeCommittedRows()} cleans up after every test — the
 * container is shared and other classes assert exact row counts.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerCreationAtomicityTest extends PostgresTestBase {

    private static final String SUBJECT_PREFIX = "atomicity-";
    private static final int ONE_ROW = 1;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CharacterRepository characterRepository;

    private String survivingSubject;
    private String doomedSubject;

    @BeforeEach
    void freshSubjects() {
        survivingSubject = SUBJECT_PREFIX + UUID.randomUUID();
        doomedSubject = SUBJECT_PREFIX + UUID.randomUUID();
    }

    @AfterEach
    void removeCommittedRows() {
        playerRepository.findByZitadelUserIdWithCharacters(survivingSubject)
                .ifPresent(playerRepository::delete);
        playerRepository.findByZitadelUserIdWithCharacters(doomedSubject)
                .ifPresent(playerRepository::delete);
    }

    /**
     * Builds an unsaved player holding one character, the shape
     * {@code createNewPlayerWithCharacter} hands to {@code save}.
     *
     * <p>The character's public ID is a parameter so a test can force a
     * collision on {@code uq_characters_public_id}. In production
     * {@code GameCharacter.prePersist} fills it with a random UUID.
     */
    private Player aPlayerWithCharacter(String subject, UUID characterPublicId) {
        Player player = Player.builder()
                .zitadelUserId(subject)
                .displayName(subject)
                .build();

        player.addCharacter(GameCharacter.builder()
                .publicId(characterPublicId)
                .name(subject)
                .build());

        return player;
    }

    /**
     * The control. Without it the failing test below would also pass if the
     * cascade never inserted a character at all.
     */
    @Test
    void shouldCommitThePlayerAndTheCharacterTogether() {
        playerRepository.save(aPlayerWithCharacter(survivingSubject, UUID.randomUUID()));

        Player stored = playerRepository.findByZitadelUserIdWithCharacters(survivingSubject).orElseThrow();

        assertThat(stored.getId()).isNotNull();
        assertThat(stored.getCharacters()).hasSize(ONE_ROW);
        assertThat(characterRepository.findByPlayerId(stored.getId())).hasSize(ONE_ROW);
    }

    /**
     * The one that matters: the character insert fails, and the player row that
     * had already been written goes with it.
     */
    @Test
    void shouldLeaveNoPlayerRowWhenTheCharacterInsertFails() {
        UUID takenPublicId = UUID.randomUUID();
        playerRepository.save(aPlayerWithCharacter(survivingSubject, takenPublicId));

        Player doomed = aPlayerWithCharacter(doomedSubject, takenPublicId);

        assertThatThrownBy(() -> playerRepository.save(doomed))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(doomed.getId())
                .as("Hibernate stamps the IDENTITY key onto the entity, so a non-null id here "
                        + "is proof the players row really was inserted before the character failed")
                .isNotNull();
        assertThat(playerRepository.findByZitadelUserIdWithCharacters(doomedSubject)).isEmpty();
        assertThat(characterRepository.findByPublicId(takenPublicId)).isPresent();
    }
}
