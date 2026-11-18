package com.tombtale.serviceinventory.mapper;

import com.tombtale.serviceinventory.dto.ItemSetResponse;
import com.tombtale.serviceinventory.dto.event.ItemSetPayload;
import com.tombtale.serviceinventory.entity.ItemSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Turns item sets into responses and events. The names match (ADR 0017), except the event's {@code itemSetPublicId}.
 */
@Mapper(componentModel = "spring")
public interface ItemSetMapper {

    /**
     * @param itemSet the stored set
     * @return the set as the API shows it
     */
    ItemSetResponse toResponse(ItemSet itemSet);

    /**
     * @param itemSet the set after a change
     * @return the set as its events carry it
     */
    @Mapping(target = "itemSetPublicId", source = "publicId")
    ItemSetPayload toPayload(ItemSet itemSet);
}
