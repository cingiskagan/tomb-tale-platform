package com.tombtale.serviceinventory.mapper;

import com.tombtale.serviceinventory.dto.ItemSetResponse;
import com.tombtale.serviceinventory.entity.ItemSet;
import org.mapstruct.Mapper;

/**
 * Turns item sets into responses. The names match (ADR 0017), so no field needs a mapping.
 */
@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.ImplicitFunctionalInterface") // MapStruct implements it, no lambda does
public interface ItemSetMapper {

    /**
     * @param itemSet the stored set
     * @return the set as the API shows it
     */
    ItemSetResponse toResponse(ItemSet itemSet);
}
