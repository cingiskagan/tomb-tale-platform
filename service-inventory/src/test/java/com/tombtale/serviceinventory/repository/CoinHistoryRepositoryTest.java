package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.config.JpaConfig;
import com.tombtale.serviceinventory.domain.CoinReason;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.CoinHistory;
import com.tombtale.serviceinventory.support.FixedAuthorTestBase;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class CoinHistoryRepositoryTest extends FixedAuthorTestBase {

    private static final long AMOUNT = 100L;

    @Autowired
    private TestEntityManager entityManager;

    /** One trade can pay two characters, but it pays each of them once. */
    @Test
    void aReferenceIsUniquePerCharacter() {
        CharacterInventory first = entityManager.persistAndFlush(aCharacter());
        CharacterInventory second = entityManager.persistAndFlush(aCharacter());
        UUID reference = UUID.randomUUID();
        entityManager.persistAndFlush(aChange(first, reference));
        entityManager.persistAndFlush(aChange(second, reference));

        assertThatThrownBy(() -> entityManager.persistAndFlush(aChange(first, reference)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("uq_coin_history_character_reference");
    }

    private static CharacterInventory aCharacter() {
        return CharacterInventory.builder()
                .playerId(UUID.randomUUID())
                .coins(AMOUNT)
                .build();
    }

    private static CoinHistory aChange(CharacterInventory character, UUID reference) {
        return CoinHistory.builder()
                .characterInventory(character)
                .amount(AMOUNT)
                .reason(CoinReason.STARTING_COINS)
                .reference(reference)
                .build();
    }
}
