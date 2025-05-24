package org.mdental.authcore.web.mapper;

import org.mapstruct.Mapper;
import org.mdental.authcore.domain.model.Tenant;
import org.mdental.authcore.web.dto.TenantResponse;

/**
 * Mapper for tenant DTOs.
 */
@Mapper(componentModel = "spring")
public interface TenantMapper {
    /**
     * Map tenant entity to response DTO.
     *
     * @param tenant the tenant entity
     * @return the tenant response DTO
     */
    TenantResponse toTenantResponse(Tenant tenant);
}