package com.tombtale.servicecommerce.service;

import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.dto.PurchaseResponse;
import com.tombtale.servicecommerce.dto.UpdatePurchaseRequest;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.entity.PurchaseStatus;
import com.tombtale.servicecommerce.exception.InvalidStatusTransitionException;
import com.tombtale.servicecommerce.exception.PurchaseNotFoundException;
import com.tombtale.servicecommerce.mapper.PurchaseMapper;
import com.tombtale.servicecommerce.repository.PurchaseRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurchaseService}.
 *
 * <p>
 * Uses Mockito to isolate the service from the repository and mapper.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.TooManyMethods"})
class PurchaseServiceTest {

    private static final UUID PURCHASE_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String PLAYER_ID = "player-001";
    private static final String ITEM_CODE = "SWORD_IRON";
    private static final int QUANTITY = 3;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("150.0000");
    private static final BigDecimal EXPECTED_TOTAL = new BigDecimal("450.0000");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int NEW_UPDATE_QUANTITY = 5;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchaseMapper purchaseMapper;

    @InjectMocks
    private PurchaseService purchaseService;

    private static Purchase buildPurchaseEntity() {
        return Purchase.builder()
                .playerId(PLAYER_ID)
                .itemCode(ITEM_CODE)
                .quantity(QUANTITY)
                .unitPrice(UNIT_PRICE)
                .totalPrice(EXPECTED_TOTAL)
                .status(PurchaseStatus.PENDING)
                .purchasedAt(Instant.now())
                .version(0)
                .build();
    }

    private static PurchaseResponse buildPurchaseResponse() {
        return new PurchaseResponse(
                PURCHASE_ID, PLAYER_ID, ITEM_CODE, QUANTITY, UNIT_PRICE, EXPECTED_TOTAL,
                PurchaseStatus.PENDING, Instant.now());
    }

    @Test
    void shouldCreatePurchaseWithCalculatedTotalPrice() {
        CreatePurchaseRequest request = new CreatePurchaseRequest(PLAYER_ID, ITEM_CODE, QUANTITY, UNIT_PRICE);
        Purchase mappedEntity = buildPurchaseEntity();
        Purchase savedEntity = buildPurchaseEntity();
        savedEntity.setId(PURCHASE_ID);
        PurchaseResponse expectedResponse = buildPurchaseResponse();

        when(purchaseMapper.toEntity(request)).thenReturn(mappedEntity);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(savedEntity);
        when(purchaseMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        PurchaseResponse result = purchaseService.createPurchase(request);

        assertThat(result.id()).isEqualTo(PURCHASE_ID);
        assertThat(result.totalPrice()).isEqualByComparingTo(EXPECTED_TOTAL);
        verify(purchaseRepository).save(any(Purchase.class));
    }

    @Test
    void shouldFindPurchaseByIdSuccessfully() {
        Purchase entity = buildPurchaseEntity();
        entity.setId(PURCHASE_ID);
        PurchaseResponse expectedResponse = buildPurchaseResponse();

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));
        when(purchaseMapper.toResponse(entity)).thenReturn(expectedResponse);

        PurchaseResponse result = purchaseService.findPurchaseById(PURCHASE_ID);

        assertThat(result.id()).isEqualTo(PURCHASE_ID);
        assertThat(result.playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void shouldThrowWhenPurchaseNotFound() {
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.findPurchaseById(PURCHASE_ID))
                .isInstanceOf(PurchaseNotFoundException.class)
                .hasMessageContaining(PURCHASE_ID.toString());
    }

    @Test
    void shouldListPurchasesWithFilter() {
        PurchaseFilterRequest filter = new PurchaseFilterRequest(PLAYER_ID, null, null, null, null);
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);
        Purchase entity = buildPurchaseEntity();
        entity.setId(PURCHASE_ID);
        Page<Purchase> entityPage = new PageImpl<>(List.of(entity), pageable, 1);
        PurchaseResponse expectedResponse = buildPurchaseResponse();

        when(purchaseRepository.findByFilter(filter, pageable)).thenReturn(entityPage);
        when(purchaseMapper.toResponse(entity)).thenReturn(expectedResponse);

        Page<PurchaseResponse> result = purchaseService.listPurchases(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void shouldUpdatePurchaseStatus() {
        Purchase entity = buildPurchaseEntity();
        entity.setId(PURCHASE_ID);
        entity.setStatus(PurchaseStatus.PENDING);
        UpdatePurchaseRequest request = new UpdatePurchaseRequest(PurchaseStatus.COMPLETED, null);
        PurchaseResponse expectedResponse = new PurchaseResponse(
                PURCHASE_ID, PLAYER_ID, ITEM_CODE, QUANTITY, UNIT_PRICE, EXPECTED_TOTAL,
                PurchaseStatus.COMPLETED, Instant.now());

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));
        when(purchaseRepository.save(entity)).thenReturn(entity);
        when(purchaseMapper.toResponse(entity)).thenReturn(expectedResponse);

        PurchaseResponse result = purchaseService.updatePurchase(PURCHASE_ID, request);

        assertThat(result.status()).isEqualTo(PurchaseStatus.COMPLETED);
        verify(purchaseRepository).save(entity);
    }

    @Test
    void shouldUpdatePurchaseQuantityAndRecalculateTotal() {
        int newQuantity = NEW_UPDATE_QUANTITY;
        Purchase entity = buildPurchaseEntity();
        entity.setId(PURCHASE_ID);
        UpdatePurchaseRequest request = new UpdatePurchaseRequest(null, newQuantity);
        BigDecimal expectedNewTotal = UNIT_PRICE.multiply(BigDecimal.valueOf(newQuantity));
        PurchaseResponse expectedResponse = new PurchaseResponse(
                PURCHASE_ID, PLAYER_ID, ITEM_CODE, newQuantity, UNIT_PRICE, expectedNewTotal,
                PurchaseStatus.PENDING, Instant.now());

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));
        when(purchaseRepository.save(entity)).thenReturn(entity);
        when(purchaseMapper.toResponse(entity)).thenReturn(expectedResponse);

        PurchaseResponse result = purchaseService.updatePurchase(PURCHASE_ID, request);

        assertThat(result.quantity()).isEqualTo(newQuantity);
        assertThat(result.totalPrice()).isEqualByComparingTo(expectedNewTotal);
    }

    @Test
    void shouldSoftDeletePurchaseBySettingStatusToCancelled() {
        Purchase entity = buildPurchaseEntity();
        entity.setId(PURCHASE_ID);

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));
        when(purchaseRepository.save(entity)).thenReturn(entity);

        purchaseService.deletePurchase(PURCHASE_ID);

        assertThat(entity.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
        verify(purchaseRepository).save(entity);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentPurchase() {
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.deletePurchase(PURCHASE_ID))
                .isInstanceOf(PurchaseNotFoundException.class);
    }

    @Test
    void shouldRejectBackwardStatusTransition() {
        Purchase entity = buildPurchaseEntity();
        entity.setStatus(PurchaseStatus.COMPLETED);

        UpdatePurchaseRequest request = new UpdatePurchaseRequest(PurchaseStatus.PENDING, null);

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> purchaseService.updatePurchase(PURCHASE_ID, request))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("PENDING");

        assertThat(entity.getStatus()).isEqualTo(PurchaseStatus.COMPLETED);
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void shouldRejectSkippingPendingToRefunded() {
        Purchase entity = buildPurchaseEntity();
        entity.setStatus(PurchaseStatus.PENDING);

        UpdatePurchaseRequest request = new UpdatePurchaseRequest(PurchaseStatus.REFUNDED, null);

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> purchaseService.updatePurchase(PURCHASE_ID, request))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("PENDING")
                .hasMessageContaining("REFUNDED");

        assertThat(entity.getStatus()).isEqualTo(PurchaseStatus.PENDING);
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void shouldRejectCancelViaUpdateEndpoint() {
        Purchase entity = buildPurchaseEntity();
        entity.setStatus(PurchaseStatus.PENDING);

        UpdatePurchaseRequest request = new UpdatePurchaseRequest(PurchaseStatus.CANCELLED, null);

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> purchaseService.updatePurchase(PURCHASE_ID, request))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("DELETE");

        assertThat(entity.getStatus()).isEqualTo(PurchaseStatus.PENDING);
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void shouldTreatSameStatusAsNoOp() {
        Purchase entity = buildPurchaseEntity();
        entity.setStatus(PurchaseStatus.COMPLETED);

        int newQuantity = NEW_UPDATE_QUANTITY;
        UpdatePurchaseRequest request = new UpdatePurchaseRequest(PurchaseStatus.COMPLETED, newQuantity);

        BigDecimal expectedNewTotal = UNIT_PRICE.multiply(BigDecimal.valueOf(newQuantity));
        PurchaseResponse expectedResponse = new PurchaseResponse(
                PURCHASE_ID, PLAYER_ID, ITEM_CODE, newQuantity, UNIT_PRICE, expectedNewTotal,
                PurchaseStatus.COMPLETED, Instant.now());

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));
        when(purchaseRepository.save(entity)).thenReturn(entity);
        when(purchaseMapper.toResponse(entity)).thenReturn(expectedResponse);

        PurchaseResponse result = purchaseService.updatePurchase(PURCHASE_ID, request);

        assertThat(entity.getStatus()).isEqualTo(PurchaseStatus.COMPLETED);
        assertThat(result.quantity()).isEqualTo(newQuantity);
        assertThat(result.totalPrice()).isEqualByComparingTo(expectedNewTotal);
        verify(purchaseRepository).save(entity);
    }

    @Test
    void shouldAllowCancellingARefundedPurchase() {
        Purchase entity = buildPurchaseEntity();
        entity.setStatus(PurchaseStatus.REFUNDED);

        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(entity));
        when(purchaseRepository.save(entity)).thenReturn(entity);

        purchaseService.deletePurchase(PURCHASE_ID);

        assertThat(entity.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
        verify(purchaseRepository).save(entity);
    }
}
