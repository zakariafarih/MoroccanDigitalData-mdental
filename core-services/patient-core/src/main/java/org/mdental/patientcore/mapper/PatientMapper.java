package org.mdental.patientcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mdental.patientcore.model.dto.PatientRequest;
import org.mdental.patientcore.model.dto.PatientResponse;
import org.mdental.patientcore.model.dto.PatientSummaryResponse;
import org.mdental.patientcore.model.entity.Patient;

@Mapper(config = MapStructConfig.class)
public interface PatientMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    Patient toEntity(PatientRequest request);

    PatientResponse toDto(Patient entity);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    void updateEntityFromDto(PatientRequest request, @MappingTarget Patient entity);

    PatientSummaryResponse toSummaryDto(Patient entity);
}
