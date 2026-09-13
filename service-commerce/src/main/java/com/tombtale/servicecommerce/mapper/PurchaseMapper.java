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
 * <p>Two names differ between the entity and the API, and this mapper is
 * where they meet. The response's {@code id} is the entity's
 * {@code publicId} — the internal key is never exposed — and its
 * {@code purchasedAt} is the entity's {@code createdAt}.
 *
 * <p>Fields the service or the persistence layer owns ({@code totalPrice},
 * {@code status}, {@code version}, and everything inherited from
 * {@code BaseEntity}) are ignored when building an entity, so a client
 * cannot supply them.
 */
@Mapper(componentModel = "spring")
public interface PurchaseMapper {

    /**
     * Converts a persisted entity to its API response representation.
     *
     * @param purchase the JPA entity
     * @return the response DTO
     */
    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "purchasedAt", source = "createdAt")
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
     * <p>The caller must still set {@code totalPrice} and {@code status};
     * the rest is filled by JPA, by auditing, or by {@code BaseEntity} itself.
     *
     * @param request the creation request DTO
     * @return a partially-populated entity (caller must set derived fields)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", ignore = true)
    Purchase toEntity(CreatePurchaseRequest request);
}
