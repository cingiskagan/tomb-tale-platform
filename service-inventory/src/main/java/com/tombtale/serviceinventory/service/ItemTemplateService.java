package com.tombtale.serviceinventory.service;

import com.tombtale.commons.web.InvalidSortFieldException;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.dto.CreateItemTemplateRequest;
import com.tombtale.serviceinventory.dto.ItemTemplateResponse;
import com.tombtale.serviceinventory.dto.UpdateItemTemplateRequest;
import com.tombtale.serviceinventory.entity.ItemSet;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.exception.DuplicateCodeException;
import com.tombtale.serviceinventory.exception.ItemTemplateNotFoundException;
import com.tombtale.serviceinventory.exception.UnknownIconException;
import com.tombtale.serviceinventory.exception.UnknownItemSetException;
import com.tombtale.serviceinventory.mapper.ItemTemplateMapper;
import com.tombtale.serviceinventory.repository.ItemIconRepository;
import com.tombtale.serviceinventory.repository.ItemSetRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The item catalog as admins manage it: create, edit, retire and read templates.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(ItemTemplateService.class);

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("code", "name", "itemType", "price", "retired", "createdAt", "updatedAt");

    private final ItemTemplateRepository itemTemplateRepository;
    private final ItemIconRepository itemIconRepository;
    private final ItemSetRepository itemSetRepository;
    private final ItemTemplateMapper itemTemplateMapper;

    /**
     * Creates a template with its type's stack size.
     *
     * @param request the new template
     * @return the saved template
     * @throws DuplicateCodeException if another template has the code
     */
    @Transactional
    public ItemTemplateResponse createTemplate(CreateItemTemplateRequest request) {
        if (itemTemplateRepository.existsByCode(request.code())) {
            throw new DuplicateCodeException("item template", request.code());
        }
        ItemTemplate saved = itemTemplateRepository.save(ItemTemplate.builder()
                .code(request.code())
                .name(request.name())
                .itemType(request.itemType())
                .maxStackSize(ItemType.EQUIPMENT_STACK_SIZE)
                .price(request.price())
                .baseStats(Stat.fromNames(request.baseStats()))
                .assetKey(request.assetKey())
                .iconHash(knownIcon(request.iconHash()))
                .itemSet(knownSet(request.itemSetId()))
                .build());
        LOG.info("Created item template code={} id={}", saved.getCode(), saved.getPublicId());
        return itemTemplateMapper.toResponse(saved);
    }

    /**
     * Replaces what may change on a template. A retired one can still be edited, since players see its copies.
     *
     * @param publicId the template
     * @param request  its new fields and the version the edit started from
     * @return the template after the edit
     * @throws ObjectOptimisticLockingFailureException if the template changed since that version
     */
    @Transactional
    public ItemTemplateResponse updateTemplate(UUID publicId, UpdateItemTemplateRequest request) {
        ItemTemplate template = findEntity(publicId);
        if (!Objects.equals(template.getVersion(), request.version())) {
            throw new ObjectOptimisticLockingFailureException(ItemTemplate.class, publicId);
        }
        template.setName(request.name());
        template.setPrice(request.price());
        template.setBaseStats(Stat.fromNames(request.baseStats()));
        template.setAssetKey(request.assetKey());
        template.setIconHash(knownIcon(request.iconHash()));
        template.setItemSet(knownSet(request.itemSetId()));
        ItemTemplate saved = itemTemplateRepository.saveAndFlush(template);
        LOG.info("Updated item template code={} to version {}", saved.getCode(), saved.getVersion());
        return itemTemplateMapper.toResponse(saved);
    }

    /**
     * Takes a template out of the game for good. Retiring it twice changes nothing.
     *
     * @param publicId the template
     * @return the retired template
     */
    @Transactional
    public ItemTemplateResponse retireTemplate(UUID publicId) {
        ItemTemplate template = findEntity(publicId);
        if (!template.isRetired()) {
            template.setRetired(true);
            template = itemTemplateRepository.saveAndFlush(template);
            LOG.info("Retired item template code={}", template.getCode());
        }
        return itemTemplateMapper.toResponse(template);
    }

    /**
     * @param publicId the template
     * @return the template
     * @throws ItemTemplateNotFoundException if no template has the id
     */
    public ItemTemplateResponse findTemplate(UUID publicId) {
        return itemTemplateMapper.toResponse(findEntity(publicId));
    }

    /**
     * Lists every template, retired ones too.
     *
     * @param pageable the page, sorted only by the fields the list allows
     * @return one page of templates
     * @throws InvalidSortFieldException if the sort names another field
     */
    public Page<ItemTemplateResponse> listTemplates(Pageable pageable) {
        SortAllowList.check(pageable.getSort(), SORTABLE_FIELDS);
        return itemTemplateRepository.findAll(pageable).map(itemTemplateMapper::toResponse);
    }

    /** A template may have no icon, but one it names must have been uploaded. */
    private String knownIcon(String hash) {
        if (hash != null && !itemIconRepository.existsByHash(hash)) {
            throw new UnknownIconException(hash);
        }
        return hash;
    }

    private ItemSet knownSet(UUID publicId) {
        return publicId == null ? null
                : itemSetRepository.findByPublicId(publicId).orElseThrow(() -> new UnknownItemSetException(publicId));
    }

    private ItemTemplate findEntity(UUID publicId) {
        return itemTemplateRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ItemTemplateNotFoundException(publicId));
    }
}
