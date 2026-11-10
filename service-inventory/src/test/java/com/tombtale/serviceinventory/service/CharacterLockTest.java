package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.config.JpaConfig;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemGrant;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.ItemGrantRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.support.ItemTemplates;
import com.tombtale.serviceinventory.support.FixedAuthorTestBase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * A second write for a character waits until the first one commits, then reads what it wrote.
 * Real commits, so no test transaction.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ JpaConfig.class, ItemGrantService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CharacterLockTest extends FixedAuthorTestBase {

    private static final String POTION = "POTION_HEALTH";
    private static final int POTION_STACK = 10;
    private static final int FIRST_QUANTITY = 8;
    private static final int SECOND_QUANTITY = 5;
    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final Duration POLL = Duration.ofMillis(20);

    private final UUID player = UUID.randomUUID();

    @Autowired
    private ItemGrantService itemGrantService;

    @Autowired
    private CharacterInventoryRepository characterInventoryRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemGrantRepository itemGrantRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CharacterInventory character;
    private ItemTemplate potion;

    @BeforeEach
    void aCharacterAndAPotion() {
        character = characterInventoryRepository.save(CharacterInventory.builder().playerId(player).coins(0L).build());
        potion = itemTemplateRepository.save(ItemTemplates.aTemplate(POTION).maxStackSize(POTION_STACK).build());
    }

    @AfterEach
    void removeCommittedRows() {
        itemInstanceRepository.deleteAllInBatch();
        itemGrantRepository.deleteAllInBatch();
        characterInventoryRepository.delete(character);
        itemTemplateRepository.delete(potion);
    }

    @Test
    void aSecondItemGrantWaitsForTheFirstToCommit() throws Exception {
        CompletableFuture<ItemGrant> second = new TransactionTemplate(transactionManager).execute(first -> {
            itemGrantService.findOrCreateItemGrant(aPurchase(FIRST_QUANTITY));
            CompletableFuture<ItemGrant> waiting = CompletableFuture.supplyAsync(
                    () -> itemGrantService.findOrCreateItemGrant(aPurchase(SECOND_QUANTITY)));
            awaitALockWait();
            return waiting;
        });

        second.get(WAIT.toSeconds(), TimeUnit.SECONDS);
        // The second grant filled the first one's stack, which no other transaction sees before the first commits.
        assertThat(stackSizes()).containsExactly(POTION_STACK, FIRST_QUANTITY + SECOND_QUANTITY - POTION_STACK);
    }

    private PurchaseCreatedPayload aPurchase(int quantity) {
        return new PurchaseCreatedPayload(UUID.randomUUID(), player, POTION, quantity);
    }

    /** Returns once some transaction waits for a lock another one holds, which here only the second grant can do. */
    private void awaitALockWait() {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (jdbcTemplate.queryForObject("select count(*) from pg_locks where not granted", Long.class) == 0) {
            if (System.nanoTime() > deadline) {
                fail("The second grant never waited for the character lock");
            }
            LockSupport.parkNanos(POLL.toNanos());
        }
    }

    private List<Integer> stackSizes() {
        return itemInstanceRepository.findAll(Sort.by("id")).stream()
                .filter(stack -> character.getId().equals(stack.getCharacterInventory().getId()))
                .map(ItemInstance::getQuantity)
                .toList();
    }
}
