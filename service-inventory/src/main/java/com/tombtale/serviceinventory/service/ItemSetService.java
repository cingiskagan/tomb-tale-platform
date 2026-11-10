package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.dto.CreateItemSetRequest;
import com.tombtale.serviceinventory.dto.ItemSetResponse;
import com.tombtale.serviceinventory.dto.UpdateItemSetRequest;
import com.tombtale.serviceinventory.entity.ItemSet;
import com.tombtale.serviceinventory.exception.DuplicateCodeException;
import com.tombtale.serviceinventory.exception.ItemSetNotFoundException;
import com.tombtale.serviceinventory.mapper.ItemSetMapper;
import com.tombtale.serviceinventory.repository.ItemSetRepository;
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
 * Item sets as admins manage them. A template joins a set through its own edit.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemSetService {

    private static final Logger LOG = LoggerFactory.getLogger(ItemSetService.class);

    private static final Set<String> SORTABLE_FIELDS = Set.of("code", "name", "createdAt", "updatedAt");

    private final ItemSetRepository itemSetRepository;
    private final ItemSetMapper itemSetMapper;

    /**
     * @param request the new set
     * @return the saved set
     * @throws DuplicateCodeException if another set has the code
     */
    @Transactional
    public ItemSetResponse createSet(CreateItemSetRequest request) {
        if (itemSetRepository.existsByCode(request.code())) {
            throw new DuplicateCodeException("item set", request.code());
        }
        ItemSet saved = itemSetRepository.save(ItemSet.builder()
                .code(request.code())
                .name(request.name())
                .bonus(Stat.fromNames(request.bonus()))
                .build());
        LOG.info("Created item set code={} id={}", saved.getCode(), saved.getPublicId());
        return itemSetMapper.toResponse(saved);
    }

    /**
     * @param publicId the set
     * @param request  its new fields and the version the edit started from
     * @return the set after the edit
     * @throws ObjectOptimisticLockingFailureException if the set changed since that version
     */
    @Transactional
    public ItemSetResponse updateSet(UUID publicId, UpdateItemSetRequest request) {
        ItemSet itemSet = findEntity(publicId);
        if (!Objects.equals(itemSet.getVersion(), request.version())) {
            throw new ObjectOptimisticLockingFailureException(ItemSet.class, publicId);
        }
        itemSet.setName(request.name());
        itemSet.setBonus(Stat.fromNames(request.bonus()));
        ItemSet saved = itemSetRepository.saveAndFlush(itemSet);
        LOG.info("Updated item set code={} to version {}", saved.getCode(), saved.getVersion());
        return itemSetMapper.toResponse(saved);
    }

    /**
     * @param publicId the set
     * @return the set
     * @throws ItemSetNotFoundException if no set has the id
     */
    public ItemSetResponse findSet(UUID publicId) {
        return itemSetMapper.toResponse(findEntity(publicId));
    }

    /**
     * @param pageable the page, sorted only by the fields the list allows
     * @return one page of sets
     */
    public Page<ItemSetResponse> listSets(Pageable pageable) {
        SortAllowList.check(pageable.getSort(), SORTABLE_FIELDS);
        return itemSetRepository.findAll(pageable).map(itemSetMapper::toResponse);
    }

    private ItemSet findEntity(UUID publicId) {
        return itemSetRepository.findByPublicId(publicId).orElseThrow(() -> new ItemSetNotFoundException(publicId));
    }
}
