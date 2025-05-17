package org.mdental.cliniccore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mdental.cliniccore.mapper.config.MapStructConfig;
import org.mdental.cliniccore.model.dto.BusinessHoursRequest;
import org.mdental.cliniccore.model.dto.BusinessHoursResponse;
import org.mdental.cliniccore.model.entity.BusinessHours;

@Mapper(config = MapStructConfig.class, componentModel = "spring")
public interface BusinessHoursMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "sequence", ignore = true) // Removing sequence=1 constant
    @Mapping(target = "validFrom", ignore = true)
    @Mapping(target = "validTo", ignore = true)
    @Mapping(target = "label", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    BusinessHours toEntity(BusinessHoursRequest request);

    BusinessHoursResponse toDto(BusinessHours entity);
}