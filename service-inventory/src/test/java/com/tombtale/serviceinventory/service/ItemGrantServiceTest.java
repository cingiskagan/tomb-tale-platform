package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.config.JpaConfig;
import com.tombtale.serviceinventory.domain.ItemGrantOutcome;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemGrant;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.ItemGrantRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.support.FixedAuthorTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ JpaConfig.class, ItemGrantService.class })
@SuppressWarnings("PMD.TooManyMethods") // the test-data builders push the count past PMD's 10
class ItemGrantServiceTest extends FixedAuthorTestBase {

    private static final String POTION = "POTION_HEALTH";
    private static final String SWORD = "SWORD_IRON";
    private static final int POTION_STACK = 10;
    private static final int STACK_WITH_ROOM = 8;
    private static final int QUANTITY = 5;
    private static final int SWORD_COPIES = 3;

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
    private TestEntityManager entityManager;

    private CharacterInventory character;

    @BeforeEach
    void aCharacterOfThePlayer() {
        character = aCharacterOf(player);
    }

    @Test
    void fillsTheStackWithRoomThenOpensANewOne() {
        ItemTemplate potion = aTemplate(POTION, POTION_STACK);
        aStack(character, potion, STACK_WITH_ROOM);

        ItemGrant itemGrant = itemGrantService.findOrCreateItemGrant(aPurchase(POTION, QUANTITY));

        assertThat(itemGrant.getOutcome()).isEqualTo(ItemGrantOutcome.GRANTED);
        assertThat(itemGrant.getCharacterId()).isEqualTo(character.getPublicId());
        assertThat(stackSizes(character)).containsExactly(POTION_STACK, STACK_WITH_ROOM + QUANTITY - POTION_STACK);
    }

    @Test
    void leavesAnotherPlayersStackWithRoomAlone() {
        ItemTemplate potion = aTemplate(POTION, POTION_STACK);
        CharacterInventory someoneElses = aCharacterOf(UUID.randomUUID());
        aStack(someoneElses, potion, STACK_WITH_ROOM);

        itemGrantService.findOrCreateItemGrant(aPurchase(POTION, QUANTITY));

        assertThat(stackSizes(someoneElses)).containsExactly(STACK_WITH_ROOM);
        assertThat(stackSizes(character)).containsExactly(QUANTITY);
    }

    @Test
    void givesEachCopyOfAnItemThatDoesNotStackItsOwnRow() {
        aTemplate(SWORD, 1);

        itemGrantService.findOrCreateItemGrant(aPurchase(SWORD, SWORD_COPIES));

        assertThat(stackSizes(character)).containsExactly(1, 1, 1);
    }

    @Test
    void givesARedeliveredPurchaseItsFirstItemGrant() {
        aTemplate(POTION, POTION_STACK);
        PurchaseCreatedPayload purchase = aPurchase(POTION, QUANTITY);

        ItemGrant first = itemGrantService.findOrCreateItemGrant(purchase);
        ItemGrant second = itemGrantService.findOrCreateItemGrant(purchase);

        assertThat(second.getPublicId()).isEqualTo(first.getPublicId());
        assertThat(stackSizes(character)).containsExactly(QUANTITY);
        assertThat(itemGrantRepository.findByPurchaseId(purchase.purchasePublicId())).isPresent();
    }

    @Test
    void rejectsAPlayerWithNoCharacter() {
        aTemplate(POTION, POTION_STACK);

        ItemGrant itemGrant = itemGrantService.findOrCreateItemGrant(
                new PurchaseCreatedPayload(UUID.randomUUID(), UUID.randomUUID(), POTION, QUANTITY));

        assertThat(itemGrant.getOutcome()).isEqualTo(ItemGrantOutcome.REJECTED);
        assertThat(itemGrant.getReason()).isEqualTo("NO_SINGLE_CHARACTER");
        assertThat(itemGrant.getCharacterId()).isNull();
    }

    /** Only the claim box can tell which of them the player wants. */
    @Test
    void rejectsAPlayerWithTwoCharacters() {
        aTemplate(POTION, POTION_STACK);
        CharacterInventory second = aCharacterOf(player);

        ItemGrant itemGrant = itemGrantService.findOrCreateItemGrant(aPurchase(POTION, QUANTITY));

        assertThat(itemGrant.getOutcome()).isEqualTo(ItemGrantOutcome.REJECTED);
        assertThat(itemGrant.getReason()).isEqualTo("NO_SINGLE_CHARACTER");
        assertThat(stackSizes(character)).isEmpty();
        assertThat(stackSizes(second)).isEmpty();
    }

    private CharacterInventory aCharacterOf(UUID owner) {
        return characterInventoryRepository.save(CharacterInventory.builder().playerId(owner).coins(0L).build());
    }

    private ItemTemplate aTemplate(String code, int maxStackSize) {
        return itemTemplateRepository.save(ItemTemplate.builder().code(code).maxStackSize(maxStackSize).build());
    }

    private void aStack(CharacterInventory owner, ItemTemplate template, int quantity) {
        itemInstanceRepository.save(
                ItemInstance.builder().characterInventory(owner).template(template).quantity(quantity).build());
    }

    private PurchaseCreatedPayload aPurchase(String itemCode, int quantity) {
        return new PurchaseCreatedPayload(UUID.randomUUID(), player, itemCode, quantity);
    }

    /** The character's stacks as the database holds them, oldest first. */
    private List<Integer> stackSizes(CharacterInventory owner) {
        entityManager.flush();
        entityManager.clear();
        return itemInstanceRepository.findAll(Sort.by("id")).stream()
                .filter(stack -> owner.getId().equals(stack.getCharacterInventory().getId()))
                .map(ItemInstance::getQuantity)
                .toList();
    }
}
