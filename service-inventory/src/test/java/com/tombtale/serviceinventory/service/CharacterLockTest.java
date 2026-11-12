package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.config.JpaConfig;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.dto.CharacterInventoryResponse;
import com.tombtale.serviceinventory.dto.DealerBuyRequest;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.Dealer;
import com.tombtale.serviceinventory.entity.ItemGrant;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.entity.StandEntry;
import com.tombtale.serviceinventory.exception.NotEnoughCoinsException;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.CoinHistoryRepository;
import com.tombtale.serviceinventory.repository.DealerRepository;
import com.tombtale.serviceinventory.repository.ItemGrantRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.repository.StandEntryRepository;
import com.tombtale.serviceinventory.security.InventoryCaller;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * A second write for a character waits until the first one commits, then reads what it wrote.
 * Real commits, so no test transaction.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ JpaConfig.class, ItemGrantService.class, ItemPlacementService.class, DealerTradeService.class,
        InventoryView.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
// One test per write path the lock guards, and each brings its own service and rows.
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
class CharacterLockTest extends FixedAuthorTestBase {

    private static final String POTION = "POTION_HEALTH";
    private static final int POTION_STACK = 10;
    private static final int FIRST_QUANTITY = 8;
    private static final int SECOND_QUANTITY = 5;
    private static final long SWORD_PRICE = 100L;
    private static final Duration WAIT = Duration.ofSeconds(10);
    private static final Duration POLL = Duration.ofMillis(20);
    private static final InventoryCaller ADMIN = new InventoryCaller(null, true);

    private final UUID player = UUID.randomUUID();

    @Autowired
    private ItemGrantService itemGrantService;

    @Autowired
    private ItemPlacementService itemPlacementService;

    @Autowired
    private DealerTradeService dealerTradeService;

    @Autowired
    private CharacterInventoryRepository characterInventoryRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemGrantRepository itemGrantRepository;

    @Autowired
    private CoinHistoryRepository coinHistoryRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private StandEntryRepository standEntryRepository;

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
        coinHistoryRepository.deleteAllInBatch();
        itemInstanceRepository.deleteAllInBatch();
        itemGrantRepository.deleteAllInBatch();
        standEntryRepository.deleteAllInBatch();
        dealerRepository.deleteAllInBatch();
        characterInventoryRepository.deleteById(character.getId());
        itemTemplateRepository.deleteAllInBatch();
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

    /** Without the lock, both would find the head empty, and the second commit would break the slot's constraint. */
    @Test
    void aSecondEquipFindsTheFirstOnesItemInTheSlot() throws Exception {
        ItemTemplate helmet = itemTemplateRepository.save(
                ItemTemplates.aTemplate("HELMET_IRON").itemType(ItemType.HELMET).build());
        ItemInstance first = aHelmetInCell(helmet, 0);
        ItemInstance second = aHelmetInCell(helmet, 1);

        CompletableFuture<CharacterInventoryResponse> waiting = new TransactionTemplate(transactionManager).execute(tx -> {
            equip(first);
            CompletableFuture<CharacterInventoryResponse> secondEquip = CompletableFuture.supplyAsync(() -> equip(second));
            awaitALockWait();
            return secondEquip;
        });

        CharacterInventoryResponse after = waiting.get(WAIT.toSeconds(), TimeUnit.SECONDS);
        assertThat(after.equipment().get(EquipmentSlot.HEAD).publicId()).isEqualTo(second.getPublicId());
        assertThat(after.bag()).singleElement()
                .satisfies(item -> assertThat(item.publicId()).isEqualTo(first.getPublicId()))
                .satisfies(item -> assertThat(item.bagPosition()).isEqualTo(1));
    }

    /** The coins pay for one sword and a half, so the second buy finds too few once the first commits. */
    @Test
    void twoBuysAtOnceCannotOverdraw() throws Exception {
        character.setCoins(SWORD_PRICE + SWORD_PRICE / 2);
        characterInventoryRepository.save(character);
        ItemTemplate sword = itemTemplateRepository.save(
                ItemTemplates.aTemplate("SWORD_IRON").itemType(ItemType.ONE_HANDED_WEAPON).price(SWORD_PRICE).build());
        Dealer blacksmith = dealerRepository.save(Dealer.builder().code("TOWN_BLACKSMITH").name("Blacksmith").build());
        StandEntry entry = standEntryRepository.save(
                StandEntry.builder().dealer(blacksmith).template(sword).rarity(Rarity.GRAY).build());

        CompletableFuture<CharacterInventoryResponse> second = new TransactionTemplate(transactionManager).execute(tx -> {
            buy(entry);
            CompletableFuture<CharacterInventoryResponse> secondBuy = CompletableFuture.supplyAsync(() -> buy(entry));
            awaitALockWait();
            return secondBuy;
        });

        assertThatThrownBy(() -> second.get(WAIT.toSeconds(), TimeUnit.SECONDS))
                .hasCauseInstanceOf(NotEnoughCoinsException.class);
        assertThat(characterInventoryRepository.findById(character.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getCoins()).isEqualTo(SWORD_PRICE / 2));
        assertThat(stackSizes()).containsExactly(1);
    }

    private CharacterInventoryResponse buy(StandEntry entry) {
        return dealerTradeService.buy(character.getPublicId(),
                new DealerBuyRequest(UUID.randomUUID(), entry.getPublicId(), SWORD_PRICE), ADMIN);
    }

    private ItemInstance aHelmetInCell(ItemTemplate helmet, int cell) {
        return itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(character).template(helmet).quantity(1).bagPosition(cell).build());
    }

    private CharacterInventoryResponse equip(ItemInstance helmet) {
        return itemPlacementService.equip(character.getPublicId(), EquipmentSlot.HEAD, helmet.getPublicId(), ADMIN);
    }

    private PurchaseCreatedPayload aPurchase(int quantity) {
        return new PurchaseCreatedPayload(UUID.randomUUID(), player, POTION, quantity);
    }

    /** Returns once some transaction waits for a lock another one holds, which here only the second write can do. */
    private void awaitALockWait() {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (jdbcTemplate.queryForObject("select count(*) from pg_locks where not granted", Long.class) == 0) {
            if (System.nanoTime() > deadline) {
                fail("The second write never waited for the character lock");
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
