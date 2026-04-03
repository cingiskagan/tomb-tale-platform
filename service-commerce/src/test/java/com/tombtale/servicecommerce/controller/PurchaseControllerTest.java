package com.tombtale.servicecommerce.controller;

import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.dto.PurchaseResponse;
import com.tombtale.servicecommerce.dto.UpdatePurchaseRequest;
import com.tombtale.servicecommerce.entity.PurchaseStatus;
import com.tombtale.servicecommerce.service.PurchaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurchaseController}.
 *
 * <p>Follows the project convention of testing controllers without
 * {@code @WebMvcTest} — directly instantiating with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseControllerTest {

    private static final UUID PURCHASE_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String PLAYER_ID = "player-001";
    private static final String ITEM_CODE = "SWORD_IRON";
    private static final int QUANTITY = 1;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("150.0000");
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("150.0000");
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Mock
    private PurchaseService purchaseService;

    @InjectMocks
    private PurchaseController purchaseController;

    @Test
    void shouldDelegateCreateToService() {
        CreatePurchaseRequest request = new CreatePurchaseRequest(PLAYER_ID, ITEM_CODE, QUANTITY, UNIT_PRICE);
        PurchaseResponse expectedResponse = buildResponse();

        when(purchaseService.createPurchase(request)).thenReturn(expectedResponse);

        PurchaseResponse result = purchaseController.createPurchase(request);

        assertThat(result.id()).isEqualTo(PURCHASE_ID);
        verify(purchaseService).createPurchase(request);
    }

    @Test
    void shouldDelegateFindByIdToService() {
        PurchaseResponse expectedResponse = buildResponse();

        when(purchaseService.findPurchaseById(PURCHASE_ID)).thenReturn(expectedResponse);

        PurchaseResponse result = purchaseController.findPurchaseById(PURCHASE_ID);

        assertThat(result.id()).isEqualTo(PURCHASE_ID);
        assertThat(result.itemCode()).isEqualTo(ITEM_CODE);
    }

    @Test
    void shouldDelegateListToService() {
        PurchaseFilterRequest filter = new PurchaseFilterRequest(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);
        Page<PurchaseResponse> expectedPage = new PageImpl<>(List.of(buildResponse()), pageable, 1);

        when(purchaseService.listPurchases(filter, pageable)).thenReturn(expectedPage);

        Page<PurchaseResponse> result = purchaseController.listPurchases(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldDelegateUpdateToService() {
        UpdatePurchaseRequest request = new UpdatePurchaseRequest(PurchaseStatus.COMPLETED, null);
        PurchaseResponse expectedResponse = new PurchaseResponse(
                PURCHASE_ID, PLAYER_ID, ITEM_CODE, QUANTITY, UNIT_PRICE, TOTAL_PRICE,
                PurchaseStatus.COMPLETED, Instant.now()
        );

        when(purchaseService.updatePurchase(PURCHASE_ID, request)).thenReturn(expectedResponse);

        PurchaseResponse result = purchaseController.updatePurchase(PURCHASE_ID, request);

        assertThat(result.status()).isEqualTo(PurchaseStatus.COMPLETED);
    }

    @Test
    void shouldDelegateDeleteToService() {
        purchaseController.deletePurchase(PURCHASE_ID);

        verify(purchaseService).deletePurchase(PURCHASE_ID);
    }

    private static PurchaseResponse buildResponse() {
        return new PurchaseResponse(
                PURCHASE_ID, PLAYER_ID, ITEM_CODE, QUANTITY, UNIT_PRICE, TOTAL_PRICE,
                PurchaseStatus.PENDING, Instant.now()
        );
    }
}
