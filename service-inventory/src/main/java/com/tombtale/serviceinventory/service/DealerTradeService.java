package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.domain.BagCells;
import com.tombtale.serviceinventory.domain.CoinReason;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.dto.CharacterInventoryResponse;
import com.tombtale.serviceinventory.dto.DealerBuyRequest;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.CoinHistory;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.StandEntry;
import com.tombtale.serviceinventory.exception.CharacterInDungeonException;
import com.tombtale.serviceinventory.exception.CharacterNotFoundException;
import com.tombtale.serviceinventory.exception.NotEnoughCoinsException;
import com.tombtale.serviceinventory.exception.PriceChangedException;
import com.tombtale.serviceinventory.exception.StandEntryNotFoundException;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.CoinHistoryRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.StandEntryRepository;
import com.tombtale.serviceinventory.security.InventoryCaller;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * A character's trades with dealers, each under the character lock. The coin history keeps every trade under its
 * trade id, so a trade sent twice happens once.
 */
@Service
@RequiredArgsConstructor
public class DealerTradeService {

    private static final Logger LOG = LoggerFactory.getLogger(DealerTradeService.class);

    private final CharacterInventoryRepository characterInventoryRepository;
    private final CoinHistoryRepository coinHistoryRepository;
    private final StandEntryRepository standEntryRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final InventoryView inventoryView;

    // A guessable sequence would let a player time a buy for a better roll.
    private final RandomGenerator random = new SecureRandom();

    /**
     * Charges the entry's price and puts a new copy in the lowest free cell. A trade id sent again changes nothing.
     *
     * @param characterId the character
     * @param request     the trade id, the stand entry and the price the player saw
     * @param caller      who asks
     * @return the inventory after the buy
     * @throws CharacterInDungeonException if the character holds a dungeon lock
     * @throws StandEntryNotFoundException if no stand shows the entry
     * @throws PriceChangedException       if the entry's price is no longer the one the player saw
     * @throws NotEnoughCoinsException     if the character can't pay it
     */
    @Transactional
    public CharacterInventoryResponse buy(UUID characterId, DealerBuyRequest request, InventoryCaller caller) {
        CharacterInventory character = lock(characterId, caller);
        if (coinHistoryRepository.existsByCharacterInventoryAndReference(character, request.tradeId())) {
            return inventoryOf(character);
        }
        checkNotInDungeon(character);
        StandEntry entry = standEntryRepository.findOnSale(request.entryId())
                .orElseThrow(() -> new StandEntryNotFoundException(request.entryId()));
        long price = entry.price();
        if (price != request.price()) {
            throw new PriceChangedException(price, request.price());
        }
        if (character.getCoins() < price) {
            throw new NotEnoughCoinsException(character.getCoins(), price);
        }
        changeCoins(character, -price, CoinReason.DEALER_BUY, request.tradeId());
        itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(character)
                .template(entry.getTemplate())
                .rarity(entry.getRarity())
                .bonus(bonusOf(entry))
                .quantity(1)
                .bagPosition(BagCells.free(itemInstanceRepository.findBagPositions(character)).nextInt())
                .build());
        LOG.info("Character {} bought {} {} for {} coins in trade {}",
                characterId, entry.getRarity(), entry.getTemplate().getCode(), price, request.tradeId());
        return inventoryOf(character);
    }

    /** A custom entry's every copy carries its bonus, and any other copy rolls its own. */
    private Map<Stat, Integer> bonusOf(StandEntry entry) {
        return entry.getBonus() == null
                ? entry.getTemplate().getItemType().bonusPool().roll(entry.getRarity(), random)
                : Map.copyOf(entry.getBonus());
    }

    private void changeCoins(CharacterInventory character, long amount, CoinReason reason, UUID tradeId) {
        character.setCoins(character.getCoins() + amount);
        coinHistoryRepository.save(CoinHistory.builder()
                .characterInventory(character)
                .amount(amount)
                .reason(reason)
                .reference(tradeId)
                .build());
    }

    private CharacterInventory lock(UUID characterId, InventoryCaller caller) {
        CharacterInventory character = characterInventoryRepository.findByPublicIdForUpdate(characterId)
                .orElseThrow(() -> new CharacterNotFoundException(characterId));
        caller.checkMayActOn(character);
        return character;
    }

    private static void checkNotInDungeon(CharacterInventory character) {
        if (character.inDungeonAt(Instant.now())) {
            throw new CharacterInDungeonException(character.getPublicId(), character.getDungeonLockUntil());
        }
    }

    private CharacterInventoryResponse inventoryOf(CharacterInventory character) {
        return inventoryView.build(character, itemInstanceRepository.findByCharacterInventory(character));
    }
}
