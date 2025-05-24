package org.mdental.cliniccore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mdental.cliniccore.mapper.config.MapStructConfig;
import org.mdental.cliniccore.model.dto.ClinicResponse;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;

@Mapper(config = MapStructConfig.class,
        componentModel = "spring", uses = {
        ContactInfoMapper.class,
        AddressMapper.class,
        BusinessHoursMapper.class,
        HolidayMapper.class
})
public interface ClinicMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "contactInfos", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "businessHours", ignore = true)
    @Mapping(target = "holidays", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "version", ignore = true)

    @Mapping(target = "kcIssuer", ignore = true)
    @Mapping(target = "kcAdminUser", ignore = true)
    @Mapping(target = "kcTmpPassword", ignore = true)

    Clinic toEntity(CreateClinicRequest request);

    ClinicResponse toDto(Clinic clinic);
}