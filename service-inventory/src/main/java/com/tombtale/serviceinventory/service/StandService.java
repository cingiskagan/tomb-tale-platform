package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.controller.ItemIconController;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.dto.StandEntryRequest;
import com.tombtale.serviceinventory.dto.StandEntryResponse;
import com.tombtale.serviceinventory.dto.StandResponse;
import com.tombtale.serviceinventory.entity.Dealer;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.entity.StandEntry;
import com.tombtale.serviceinventory.exception.CustomStandItemsDisabledException;
import com.tombtale.serviceinventory.exception.DealerNotFoundException;
import com.tombtale.serviceinventory.exception.RetiredTemplateException;
import com.tombtale.serviceinventory.exception.StandEntryNotFoundException;
import com.tombtale.serviceinventory.exception.UnknownItemTemplateException;
import com.tombtale.serviceinventory.repository.DealerRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.repository.StandEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Dealers' stands: anyone reads one, and admins add and remove its entries. A stand is found by its dealer's code.
 */
@Service
@Transactional(readOnly = true)
public class StandService {

    private static final Logger LOG = LoggerFactory.getLogger(StandService.class);

    private final DealerRepository dealerRepository;
    private final StandEntryRepository standEntryRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final boolean customStandItemsEnabled;

    /** Custom entries are allowed only while {@code inventory.custom-stand-items.enabled} is on. */
    public StandService(
            DealerRepository dealerRepository,
            StandEntryRepository standEntryRepository,
            ItemTemplateRepository itemTemplateRepository,
            @Value("${inventory.custom-stand-items.enabled}") boolean customStandItemsEnabled) {
        this.dealerRepository = dealerRepository;
        this.standEntryRepository = standEntryRepository;
        this.itemTemplateRepository = itemTemplateRepository;
        this.customStandItemsEnabled = customStandItemsEnabled;
    }

    /**
     * @param dealerCode the dealer's code
     * @return its stand, without retired templates, and empty while the dealer is retired
     * @throws DealerNotFoundException if no dealer has the code
     */
    public StandResponse findStand(String dealerCode) {
        Dealer dealer = findDealer(dealerCode);
        List<StandEntry> entries = dealer.isRetired() ? List.of() : standEntryRepository.findOnStand(dealer);
        return new StandResponse(dealer.getCode(), dealer.getName(),
                entries.stream().map(StandService::toResponse).toList());
    }

    /**
     * @param dealerCode the dealer's code
     * @param request    the template, the rarity and, for a custom entry, its bonus
     * @return the saved entry
     * @throws CustomStandItemsDisabledException if the request has a bonus while the setting is off
     * @throws RetiredTemplateException          if the template is retired
     */
    @Transactional
    public StandEntryResponse addEntry(String dealerCode, StandEntryRequest request) {
        if (request.bonus() != null && !customStandItemsEnabled) {
            throw new CustomStandItemsDisabledException();
        }
        Dealer dealer = findDealer(dealerCode);
        ItemTemplate template = itemTemplateRepository.findByPublicId(request.templateId())
                .orElseThrow(() -> new UnknownItemTemplateException(request.templateId()));
        if (template.isRetired()) {
            throw new RetiredTemplateException(template.getCode());
        }
        StandEntry saved = standEntryRepository.save(StandEntry.builder()
                .dealer(dealer)
                .template(template)
                .rarity(request.rarity())
                .bonus(request.bonus() == null ? null : Stat.fromNames(request.bonus()))
                .build());
        LOG.info("Added {} {} to the stand of {}{}", request.rarity(), template.getCode(), dealer.getCode(),
                saved.getBonus() == null ? "" : " with a custom bonus");
        return toResponse(saved);
    }

    /**
     * @param dealerCode the dealer's code
     * @param entryId    the entry on its stand
     * @throws StandEntryNotFoundException if the stand has no entry with the id
     */
    @Transactional
    public void removeEntry(String dealerCode, UUID entryId) {
        Dealer dealer = findDealer(dealerCode);
        StandEntry entry = standEntryRepository.findByPublicIdAndDealer(entryId, dealer)
                .orElseThrow(() -> new StandEntryNotFoundException(entryId));
        standEntryRepository.delete(entry);
        LOG.info("Removed entry id={} from the stand of {}", entryId, dealer.getCode());
    }

    private Dealer findDealer(String code) {
        return dealerRepository.findByCode(code).orElseThrow(() -> new DealerNotFoundException(code));
    }

    private static StandEntryResponse toResponse(StandEntry entry) {
        ItemTemplate template = entry.getTemplate();
        return new StandEntryResponse(
                entry.getPublicId(),
                template.getCode(),
                template.getName(),
                template.getItemType(),
                ItemIconController.urlOf(template.getIconHash()),
                template.getBaseStats(),
                entry.getRarity(),
                entry.getBonus(),
                entry.price());
    }
}
