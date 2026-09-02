package com.tombtale.servicecommerce.repository;

import com.tombtale.servicecommerce.config.QueryDslConfig;
import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.entity.PurchaseStatus;
import com.tombtale.servicecommerce.support.PostgresTestBase;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link PurchaseQueryRepositoryImpl} against a real
 * Postgres started by Testcontainers.
 *
 * <p>The repository is injected as {@link PurchaseRepository} — the composed
 * Spring Data proxy that callers actually receive — so exception translation
 * and the base fragment are in play exactly as they are in production.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
// Test-data builders push the method count past PMD's default of 10; splitting a
// cohesive test class to satisfy a counter would not improve it.
@SuppressWarnings("PMD.TooManyMethods")
class PurchaseQueryRepositoryImplTest extends PostgresTestBase {

    private static final int PAGE_SIZE = 10;
    private static final int DAYS_AGO = 10;

    private static final String PLAYER_ONE = "Player_0";
    private static final String PLAYER_TWO = "Player_1";

    private static final String ARKENSTONE = "arkenstone";
    private static final String NENYA = "nenya";
    private static final String SILMARIL = "silmaril";
    private static final String ONERING = "onering";

    private static final String PRICE_HIGH = "999999.0000";
    private static final String PRICE_LOW = "100000.0000";

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSeeNoSeedData() {
        Page<Purchase> page = purchaseRepository.findByFilter(noFilter(), PageRequest.of(0, PAGE_SIZE));

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void shouldExcludeCancelledByDefault() {
        Instant now = nowInDatabasePrecision();
        persist(
                aPurchase(PLAYER_ONE, ARKENSTONE, 1, PRICE_HIGH, PurchaseStatus.CANCELLED, now),
                aPurchase(PLAYER_ONE, NENYA, 1, PRICE_LOW, PurchaseStatus.COMPLETED, now));

        Page<Purchase> page = purchaseRepository.findByFilter(noFilter(), PageRequest.of(0, PAGE_SIZE));

        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent()).extracting(Purchase::getStatus).containsExactly(PurchaseStatus.COMPLETED);
    }

    @Test
    void shouldIncludeCancelledWhenAsked() {
        Instant now = nowInDatabasePrecision();
        persist(
                aPurchase(PLAYER_ONE, ARKENSTONE, 1, PRICE_HIGH, PurchaseStatus.CANCELLED, now),
                aPurchase(PLAYER_ONE, NENYA, 1, PRICE_LOW, PurchaseStatus.COMPLETED, now));

        Page<Purchase> page = purchaseRepository.findByFilter(
                byStatus(PurchaseStatus.CANCELLED), PageRequest.of(0, PAGE_SIZE));

        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent()).extracting(Purchase::getStatus).containsExactly(PurchaseStatus.CANCELLED);
    }

    @Test
    void shouldFilterByDateRange() {
        Instant now = nowInDatabasePrecision();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tenDaysAgo = now.minus(DAYS_AGO, ChronoUnit.DAYS);

        persist(
                aPurchase(PLAYER_ONE, SILMARIL, 2, PRICE_HIGH, PurchaseStatus.PENDING, tenDaysAgo),
                aPurchase(PLAYER_ONE, ONERING, 2, PRICE_LOW, PurchaseStatus.REFUNDED, now));

        Page<Purchase> page = purchaseRepository.findByFilter(
                purchasedBetween(yesterday, now), PageRequest.of(0, PAGE_SIZE));

        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent()).extracting(Purchase::getPurchasedAt).containsExactly(now);
    }

    @Test
    void shouldFilterByPlayerIdAndItemCode() {
        Instant now = nowInDatabasePrecision();
        persist(
                aPurchase(PLAYER_ONE, ARKENSTONE, 1, PRICE_HIGH, PurchaseStatus.COMPLETED, now),
                aPurchase(PLAYER_ONE, NENYA, 1, PRICE_LOW, PurchaseStatus.COMPLETED, now),
                aPurchase(PLAYER_TWO, SILMARIL, 2, PRICE_HIGH, PurchaseStatus.PENDING, now));

        Page<Purchase> page = purchaseRepository.findByFilter(
                byPlayerAndItem(PLAYER_ONE, NENYA), PageRequest.of(0, PAGE_SIZE));

        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent()).extracting(Purchase::getItemCode).containsExactly(NENYA);
    }

    /**
     * An unknown sort property is rejected by the allow-list before it reaches the
     * query, so it can never steer the generated HQL. Mirrors the same guard in
     * player, down to the message.
     */
    @Test
    void shouldRejectUnknownSortField() {
        PageRequest pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("nonsense"));

        assertThatThrownBy(() -> purchaseRepository.findByFilter(noFilter(), pageable))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field: nonsense");
    }

    /**
     * The allow-list rejects an HQL-injection attempt through {@code ?sort=} rather
     * than letting the payload reach {@code PathBuilder}, which does not validate it.
     */
    @Test
    void shouldRejectInjectionAttemptInSortField() {
        PageRequest pageable = PageRequest.of(
                0, PAGE_SIZE, Sort.by("id, (select p.playerId from Purchase p)"));

        assertThatThrownBy(() -> purchaseRepository.findByFilter(noFilter(), pageable))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field:");
    }

    /**
     * Every allow-listed field is genuinely sortable — the guard rejects bad input
     * without also blocking the columns callers are meant to use.
     */
    @Test
    void shouldAcceptEveryAllowedSortField() {
        persist(aPurchase(PLAYER_ONE, ARKENSTONE, 1, PRICE_HIGH,
                PurchaseStatus.COMPLETED, nowInDatabasePrecision()));

        for (String field : new String[] {
            "id", "playerId", "itemCode", "quantity",
            "unitPrice", "totalPrice", "status", "purchasedAt", }) {
            PageRequest pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(field));

            assertThat(purchaseRepository.findByFilter(noFilter(), pageable).getContent())
                    .as("sorting by %s", field)
                    .hasSize(1);
        }
    }

    /**
     * Saves the given rows, then flushes and clears the persistence context.
     *
     * <p>The clear matters: without it the repository would answer from
     * Hibernate's first-level cache and the test would never touch Postgres.
     * Clearing forces every later read to go to the database, which is the
     * whole point of running these tests against a real one.
     */
    private void persist(Purchase... purchases) {
        for (Purchase purchase : purchases) {
            entityManager.persist(purchase);
        }
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * {@code purchased_at} is {@code timestamp(6)}, so Postgres keeps microseconds.
     * Truncating here makes what we assert equal to what comes back.
     */
    private static Instant nowInDatabasePrecision() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static Purchase aPurchase(String playerId, String itemCode, int quantity,
            String unitPrice, PurchaseStatus status, Instant purchasedAt) {
        BigDecimal price = new BigDecimal(unitPrice);
        return Purchase.builder()
                .playerId(playerId)
                .itemCode(itemCode)
                .quantity(quantity)
                .unitPrice(price)
                .totalPrice(price.multiply(BigDecimal.valueOf(quantity)))
                .status(status)
                .purchasedAt(purchasedAt)
                .build();
    }

    private static PurchaseFilterRequest noFilter() {
        return new PurchaseFilterRequest(null, null, null, null, null);
    }

    private static PurchaseFilterRequest byStatus(PurchaseStatus status) {
        return new PurchaseFilterRequest(null, null, status, null, null);
    }

    private static PurchaseFilterRequest purchasedBetween(Instant after, Instant before) {
        return new PurchaseFilterRequest(null, null, null, after, before);
    }

    private static PurchaseFilterRequest byPlayerAndItem(String playerId, String itemCode) {
        return new PurchaseFilterRequest(playerId, itemCode, null, null, null);
    }
}
