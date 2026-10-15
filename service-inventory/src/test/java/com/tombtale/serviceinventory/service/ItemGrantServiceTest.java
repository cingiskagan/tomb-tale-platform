package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.config.JpaConfig;
import com.tombtale.serviceinventory.domain.ItemGrantOutcome;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
import com.tombtale.serviceinventory.entity.ItemGrant;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.ItemGrantRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.support.FixedAuthorTestBase;

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
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemGrantRepository itemGrantRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void fillsTheStackWithRoomThenOpensANewOne() {
        ItemTemplate potion = aTemplate(POTION, POTION_STACK);
        itemInstanceRepository.save(
                ItemInstance.builder().playerId(player).template(potion).quantity(STACK_WITH_ROOM).build());

        ItemGrant itemGrant = itemGrantService.findOrCreateItemGrant(aPurchase(POTION, QUANTITY));

        assertThat(itemGrant.getOutcome()).isEqualTo(ItemGrantOutcome.GRANTED);
        assertThat(stackSizes()).containsExactly(POTION_STACK, STACK_WITH_ROOM + QUANTITY - POTION_STACK);
    }

    @Test
    void givesEachCopyOfAnItemThatDoesNotStackItsOwnRow() {
        aTemplate(SWORD, 1);

        itemGrantService.findOrCreateItemGrant(aPurchase(SWORD, SWORD_COPIES));

        assertThat(stackSizes()).containsExactly(1, 1, 1);
    }

    @Test
    void givesARedeliveredPurchaseItsFirstItemGrant() {
        aTemplate(POTION, POTION_STACK);
        PurchaseCreatedPayload purchase = aPurchase(POTION, QUANTITY);

        ItemGrant first = itemGrantService.findOrCreateItemGrant(purchase);
        ItemGrant second = itemGrantService.findOrCreateItemGrant(purchase);

        assertThat(second.getPublicId()).isEqualTo(first.getPublicId());
        assertThat(stackSizes()).containsExactly(QUANTITY);
        assertThat(itemGrantRepository.findByPurchaseId(purchase.purchasePublicId())).isPresent();
    }

    private ItemTemplate aTemplate(String code, int maxStackSize) {
        return itemTemplateRepository.save(ItemTemplate.builder().code(code).maxStackSize(maxStackSize).build());
    }

    private PurchaseCreatedPayload aPurchase(String itemCode, int quantity) {
        return new PurchaseCreatedPayload(UUID.randomUUID(), player, itemCode, quantity);
    }

    /** The player's stacks as the database holds them, oldest first. */
    private List<Integer> stackSizes() {
        entityManager.flush();
        entityManager.clear();
        return itemInstanceRepository.findAll(Sort.by("id")).stream()
                .filter(stack -> player.equals(stack.getPlayerId()))
                .map(ItemInstance::getQuantity)
                .toList();
    }
}
