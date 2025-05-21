package org.mdental.patientcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mdental.patientcore.model.dto.ContactInfoRequest;
import org.mdental.patientcore.model.dto.ContactInfoResponse;
import org.mdental.patientcore.model.entity.ContactInfo;

@Mapper(config = MapStructConfig.class)
public interface ContactInfoMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    ContactInfo toEntity(ContactInfoRequest request);

    ContactInfoResponse toDto(ContactInfo entity);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    void updateEntityFromDto(ContactInfoRequest request, @MappingTarget ContactInfo entity);
}
