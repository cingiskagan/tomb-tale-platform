package com.tombtale.serviceinventory.mapper;

import com.tombtale.serviceinventory.dto.ItemTemplateResponse;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import org.mapstruct.Mapper;

/**
 * Turns templates into responses. The names match (ADR 0017), so no field needs a mapping.
 */
@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.ImplicitFunctionalInterface") // MapStruct implements it, no lambda does
public interface ItemTemplateMapper {

    /**
     * @param template the stored template
     * @return the template as the API shows it
     */
    ItemTemplateResponse toResponse(ItemTemplate template);
}
