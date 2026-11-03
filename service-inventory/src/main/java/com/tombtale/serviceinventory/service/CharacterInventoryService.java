package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.domain.CoinReason;
import com.tombtale.serviceinventory.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.CoinHistory;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.CoinHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the inventory of every new character, with its starting coins.
 */
@Service
public class CharacterInventoryService {

    private final CharacterInventoryRepository characterInventoryRepository;
    private final CoinHistoryRepository coinHistoryRepository;
    private final long startingCoins;

    /** The starting coins come from {@code inventory.starting-coins} in application.yml. */
    public CharacterInventoryService(
            CharacterInventoryRepository characterInventoryRepository,
            CoinHistoryRepository coinHistoryRepository,
            @Value("${inventory.starting-coins}") long startingCoins) {
        this.characterInventoryRepository = characterInventoryRepository;
        this.coinHistoryRepository = coinHistoryRepository;
        this.startingCoins = startingCoins;
    }

    /**
     * Saves the character's row with the starting coins and their history row, in one transaction.
     * A redelivered event finds the row and adds nothing.
     *
     * @param player the data of the delivered {@code player.created}
     */
    @Transactional
    public void createInventory(PlayerCreatedPayload player) {
        if (characterInventoryRepository.existsByPublicId(player.characterPublicId())) {
            return;
        }
        CharacterInventory inventory = characterInventoryRepository.save(CharacterInventory.builder()
                .publicId(player.characterPublicId())
                .playerId(player.playerPublicId())
                .coins(startingCoins)
                .build());
        // The reference is the character itself, so the history refuses a second start.
        coinHistoryRepository.save(CoinHistory.builder()
                .characterInventory(inventory)
                .amount(startingCoins)
                .reason(CoinReason.STARTING_COINS)
                .reference(inventory.getPublicId())
                .build());
    }
}
