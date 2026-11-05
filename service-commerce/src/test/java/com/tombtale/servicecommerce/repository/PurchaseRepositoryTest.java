package com.tombtale.servicecommerce.repository;

import com.tombtale.servicecommerce.config.JpaConfig;
import com.tombtale.servicecommerce.config.QueryDslConfig;
import com.tombtale.servicecommerce.domain.PurchaseStatus;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.support.FixedAuthorTestBase;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class })
class PurchaseRepositoryTest extends FixedAuthorTestBase {

    private static final UUID PLAYER_ONE = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    private static final String BEGINNER_SET_KEY = "beginner_set";

    private static final String ARKENSTONE = "arkenstone";

    private static final String PRICE = "999999.0000";

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldThrowWhenDuplicateSave() {
        
        Purchase purchase1 = aPurchase(PLAYER_ONE, ARKENSTONE, 1, PRICE, PurchaseStatus.COMPLETED, BEGINNER_SET_KEY);
        Purchase purchase2 = aPurchase(PLAYER_ONE, ARKENSTONE, 1, PRICE, PurchaseStatus.COMPLETED, BEGINNER_SET_KEY);

        persist(purchase1);

        assertThatThrownBy(() -> persist(purchase2))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("uq_purchases_player_id_idempotency_key");
    }

    @Test
    void shouldSaveWithNullIdempotencyKeyTwice() {
        
        Purchase purchase1 = aPurchase(PLAYER_ONE, ARKENSTONE, 1, PRICE, PurchaseStatus.COMPLETED, null);
        Purchase purchase2 = aPurchase(PLAYER_ONE, ARKENSTONE, 1, PRICE, PurchaseStatus.COMPLETED, null);

        persist(purchase1);
        persist(purchase2);

        assertThat(purchaseRepository.count()).isEqualTo(2L);
    }

    private static Purchase aPurchase(UUID playerId, String itemCode, int quantity,
            String unitPrice, PurchaseStatus status, String idempotencyKey) {
        BigDecimal price = new BigDecimal(unitPrice);
        return Purchase.builder()
                .playerId(playerId)
                .itemCode(itemCode)
                .quantity(quantity)
                .unitPrice(price)
                .totalPrice(price.multiply(BigDecimal.valueOf(quantity)))
                .status(status)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private void persist(Purchase purchase) {
        entityManager.persist(purchase);
        entityManager.flush();
        entityManager.clear();
    }
}
