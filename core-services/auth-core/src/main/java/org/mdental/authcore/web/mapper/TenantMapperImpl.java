package org.mdental.authcore.web.mapper;

import org.mapstruct.Mapper;
import org.mdental.authcore.domain.model.Tenant;
import org.mdental.authcore.web.dto.TenantResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for tenant DTOs.
 */
@Component
public class TenantMapperImpl implements TenantMapper {
    @Override
    public TenantResponse toTenantResponse(Tenant tenant) {
        if (tenant == null) {
            return null;
        }

        return new TenantResponse(
                tenant.getId(),
                tenant.getSlug(),
                tenant.getName(),
                tenant.isActive(),
                tenant.getCreatedAt()
        );
    }
}