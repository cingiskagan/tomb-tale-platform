package com.tombtale.serviceinventory.service;

import com.tombtale.commons.web.InvalidSortFieldException;
import com.tombtale.serviceinventory.dto.CreateDealerRequest;
import com.tombtale.serviceinventory.dto.DealerResponse;
import com.tombtale.serviceinventory.dto.UpdateDealerRequest;
import com.tombtale.serviceinventory.entity.Dealer;
import com.tombtale.serviceinventory.exception.DealerNotFoundException;
import com.tombtale.serviceinventory.exception.DuplicateCodeException;
import com.tombtale.serviceinventory.mapper.DealerMapper;
import com.tombtale.serviceinventory.repository.DealerRepository;
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
 * Dealers as admins manage them: create, rename, retire and read. Their stands are {@link StandService}'s.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealerService {

    private static final Logger LOG = LoggerFactory.getLogger(DealerService.class);

    private static final Set<String> SORTABLE_FIELDS = Set.of("code", "name", "retired", "createdAt", "updatedAt");

    private final DealerRepository dealerRepository;
    private final DealerMapper dealerMapper;

    /**
     * @param request the new dealer
     * @return the saved dealer
     * @throws DuplicateCodeException if another dealer, retired or not, has the code
     */
    @Transactional
    public DealerResponse createDealer(CreateDealerRequest request) {
        if (dealerRepository.existsByCode(request.code())) {
            throw new DuplicateCodeException("dealer", request.code());
        }
        Dealer saved = dealerRepository.save(Dealer.builder()
                .code(request.code())
                .name(request.name())
                .build());
        LOG.info("Created dealer code={} id={}", saved.getCode(), saved.getPublicId());
        return dealerMapper.toResponse(saved);
    }

    /**
     * @param publicId the dealer
     * @param request  its new name and the version the edit started from
     * @return the dealer after the edit
     * @throws ObjectOptimisticLockingFailureException if the dealer changed since that version
     */
    @Transactional
    public DealerResponse updateDealer(UUID publicId, UpdateDealerRequest request) {
        Dealer dealer = findEntity(publicId);
        if (!Objects.equals(dealer.getVersion(), request.version())) {
            throw new ObjectOptimisticLockingFailureException(Dealer.class, publicId);
        }
        dealer.setName(request.name());
        Dealer saved = dealerRepository.saveAndFlush(dealer);
        LOG.info("Updated dealer code={} to version {}", saved.getCode(), saved.getVersion());
        return dealerMapper.toResponse(saved);
    }

    /**
     * Empties the dealer's stand for good. Retiring it twice changes nothing.
     *
     * @param publicId the dealer
     * @return the retired dealer
     */
    @Transactional
    public DealerResponse retireDealer(UUID publicId) {
        Dealer dealer = findEntity(publicId);
        if (!dealer.isRetired()) {
            dealer.setRetired(true);
            dealer = dealerRepository.saveAndFlush(dealer);
            LOG.info("Retired dealer code={}", dealer.getCode());
        }
        return dealerMapper.toResponse(dealer);
    }

    /**
     * @param publicId the dealer
     * @return the dealer
     * @throws DealerNotFoundException if no dealer has the id
     */
    public DealerResponse findDealer(UUID publicId) {
        return dealerMapper.toResponse(findEntity(publicId));
    }

    /**
     * Lists every dealer, retired ones too.
     *
     * @param pageable the page, sorted only by the fields the list allows
     * @return one page of dealers
     * @throws InvalidSortFieldException if the sort names another field
     */
    public Page<DealerResponse> listDealers(Pageable pageable) {
        SortAllowList.check(pageable.getSort(), SORTABLE_FIELDS);
        return dealerRepository.findAll(pageable).map(dealerMapper::toResponse);
    }

    private Dealer findEntity(UUID publicId) {
        return dealerRepository.findByPublicId(publicId).orElseThrow(() -> new DealerNotFoundException(publicId));
    }
}
