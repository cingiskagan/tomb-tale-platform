package com.tombtale.servicecommerce.mapper;

import com.tombtale.servicecommerce.dto.PurchaseItemDto;
import com.tombtale.servicecommerce.dto.PurchaseOrderDto;
import com.tombtale.servicecommerce.entity.PurchaseItem;
import com.tombtale.servicecommerce.entity.PurchaseOrder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link PurchaseOrder} entities
 * and their DTO representations.
 * <p>
 * Uses Spring component model so the generated implementation is
 * auto-registered as a Spring bean and can be injected via constructor.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PurchaseOrderMapper {

    /** Maps a {@link PurchaseOrder} entity to its response DTO. */
    PurchaseOrderDto toDto(PurchaseOrder order);

    /** Maps a {@link PurchaseItem} entity to its response DTO. */
    PurchaseItemDto toItemDto(PurchaseItem item);

    /** Maps a list of {@link PurchaseOrder} entities to DTOs. */
    List<PurchaseOrderDto> toDtoList(List<PurchaseOrder> orders);
}
