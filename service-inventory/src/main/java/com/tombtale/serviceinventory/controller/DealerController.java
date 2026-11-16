package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.commons.web.PagedResponse;
import com.tombtale.serviceinventory.dto.CreateDealerRequest;
import com.tombtale.serviceinventory.dto.DealerResponse;
import com.tombtale.serviceinventory.dto.UpdateDealerRequest;
import com.tombtale.serviceinventory.service.DealerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Dealers, for {@code platform_admin} only. A dealer is retired, never deleted.
 */
@RestController
@RequestMapping("/api/v1/inventory/dealers")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.IS_ADMIN)
public class DealerController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final DealerService dealerService;

    /**
     * A taken code answers 409, even a retired dealer's.
     *
     * @param request the new dealer
     * @return the saved dealer
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealerResponse createDealer(@Valid @RequestBody CreateDealerRequest request) {
        return dealerService.createDealer(request);
    }

    /**
     * @param pageable the page, by code unless the caller sorts
     * @return one page of dealers, retired ones too
     */
    @GetMapping
    public PagedResponse<DealerResponse> listDealers(
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return PagedResponse.from(dealerService.listDealers(pageable));
    }

    /**
     * @param publicId the dealer
     * @return the dealer
     */
    @GetMapping("/{publicId}")
    public DealerResponse findDealer(@PathVariable UUID publicId) {
        return dealerService.findDealer(publicId);
    }

    /**
     * A stale version answers 409.
     *
     * @param publicId the dealer
     * @param request  its new name and the version the edit started from
     * @return the dealer after the edit
     */
    @PutMapping("/{publicId}")
    public DealerResponse updateDealer(@PathVariable UUID publicId, @Valid @RequestBody UpdateDealerRequest request) {
        return dealerService.updateDealer(publicId, request);
    }

    /**
     * A retired dealer's stand shows nothing.
     *
     * @param publicId the dealer
     * @return the retired dealer
     */
    @PostMapping("/{publicId}/retire")
    public DealerResponse retireDealer(@PathVariable UUID publicId) {
        return dealerService.retireDealer(publicId);
    }
}
