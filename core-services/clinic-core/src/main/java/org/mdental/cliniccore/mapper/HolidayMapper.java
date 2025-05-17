package org.mdental.cliniccore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mdental.cliniccore.mapper.config.MapStructConfig;
import org.mdental.cliniccore.model.dto.HolidayRequest;
import org.mdental.cliniccore.model.dto.HolidayResponse;
import org.mdental.cliniccore.model.entity.Holiday;

@Mapper(config = MapStructConfig.class,componentModel = "spring")
public interface HolidayMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "ruleType", constant = "FIXED_DATE") // Default rule type
    @Mapping(target = "rulePattern", ignore = true)
    @Mapping(target = "isHalfDay", constant = "false") // Default not half day
    @Mapping(target = "halfDayStart", ignore = true)
    @Mapping(target = "halfDayEnd", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Holiday toEntity(HolidayRequest request);

    HolidayResponse toDto(Holiday entity);
}