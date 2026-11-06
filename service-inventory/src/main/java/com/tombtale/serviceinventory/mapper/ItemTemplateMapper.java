package com.tombtale.serviceinventory.mapper;

import com.tombtale.serviceinventory.dto.ItemTemplateResponse;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Turns templates into responses. The names match (ADR 0017), and the set is named by its {@code publicId}.
 */
@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.ImplicitFunctionalInterface") // MapStruct implements it, no lambda does
public interface ItemTemplateMapper {

    /**
     * @param template the stored template
     * @return the template as the API shows it
     */
    @Mapping(target = "itemSetId", source = "itemSet.publicId")
    ItemTemplateResponse toResponse(ItemTemplate template);
}
