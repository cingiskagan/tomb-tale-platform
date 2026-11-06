package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.config.JpaConfig;
import com.tombtale.serviceinventory.entity.CharacterInventory;
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
class CharacterInventoryRepositoryTest extends FixedAuthorTestBase {

    @Autowired
    private TestEntityManager entityManager;

    /** The backup for a coin change that skips its balance check. */
    @Test
    void refusesFewerThanZeroCoins() {
        CharacterInventory overdrawn = CharacterInventory.builder()
                .playerId(UUID.randomUUID())
                .coins(-1L)
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(overdrawn))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ck_character_inventories_coins");
    }
}
