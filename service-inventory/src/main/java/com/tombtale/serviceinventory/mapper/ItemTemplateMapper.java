package com.tombtale.serviceinventory.mapper;

import com.tombtale.serviceinventory.dto.ItemTemplateResponse;
import com.tombtale.serviceinventory.dto.event.ItemTemplatePayload;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Turns templates into responses and events. The names match (ADR 0017), and the set is named by its {@code publicId}.
 */
@Mapper(componentModel = "spring")
public interface ItemTemplateMapper {

    /**
     * @param template the stored template
     * @return the template as the API shows it
     */
    @Mapping(target = "itemSetId", source = "itemSet.publicId")
    ItemTemplateResponse toResponse(ItemTemplate template);

    /**
     * @param template the template after a change
     * @return the template as its events carry it
     */
    @Mapping(target = "itemTemplatePublicId", source = "publicId")
    @Mapping(target = "itemSetPublicId", source = "itemSet.publicId")
    ItemTemplatePayload toPayload(ItemTemplate template);
}
