package com.tombtale.serviceinventory.mapper;

import com.tombtale.serviceinventory.dto.DealerResponse;
import com.tombtale.serviceinventory.entity.Dealer;
import org.mapstruct.Mapper;

/**
 * Turns dealers into responses. The names match (ADR 0017), so no field needs a mapping.
 */
@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.ImplicitFunctionalInterface") // MapStruct implements it, no lambda does
public interface DealerMapper {

    /**
     * @param dealer the stored dealer
     * @return the dealer as the API shows it
     */
    DealerResponse toResponse(Dealer dealer);
}
