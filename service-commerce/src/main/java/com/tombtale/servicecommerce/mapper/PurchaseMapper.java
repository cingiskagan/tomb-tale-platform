package com.tombtale.servicecommerce.mapper;

import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseResponse;
import com.tombtale.servicecommerce.entity.Purchase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper converting between {@link Purchase} entities and
 * their DTO representations.
 *
 * <p>Fields that are calculated or set by the service layer
 * ({@code totalPrice}, {@code status}, {@code purchasedAt}, {@code version})
 * are explicitly ignored during entity creation to prevent accidental
 * client-supplied overwrites.
 */
@Mapper(componentModel = "spring")
public interface PurchaseMapper {

    /**
     * Converts a persisted entity to its API response representation.
     *
     * @param purchase the JPA entity
     * @return the response DTO
     */
    PurchaseResponse toResponse(Purchase purchase);

    /**
     * Converts a list of entities to a list of response DTOs.
     *
     * @param purchases the entity list
     * @return the response DTO list
     */
    List<PurchaseResponse> toResponseList(List<Purchase> purchases);

    /**
     * Creates a new entity from the inbound creation request.
     *
     * <p>Service-managed fields are ignored — the service sets
     * {@code totalPrice}, {@code status}, {@code purchasedAt}, and lets
     * JPA generate the {@code id} and {@code version}.
     *
     * @param request the creation request DTO
     * @return a partially-populated entity (caller must set derived fields)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "purchasedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Purchase toEntity(CreatePurchaseRequest request);
}
