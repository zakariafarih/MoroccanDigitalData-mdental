package org.mdental.patientcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mdental.patientcore.model.dto.EmergencyContactRequest;
import org.mdental.patientcore.model.dto.EmergencyContactResponse;
import org.mdental.patientcore.model.entity.EmergencyContact;

@Mapper(config = MapStructConfig.class)
public interface EmergencyContactMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    EmergencyContact toEntity(EmergencyContactRequest request);

    EmergencyContactResponse toDto(EmergencyContact entity);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    void updateEntityFromDto(EmergencyContactRequest request, @MappingTarget EmergencyContact entity);
}
