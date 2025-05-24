package org.mdental.cliniccore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mdental.cliniccore.mapper.config.MapStructConfig;
import org.mdental.cliniccore.model.dto.ContactInfoRequest;
import org.mdental.cliniccore.model.dto.ContactInfoResponse;
import org.mdental.cliniccore.model.entity.ContactInfo;

@Mapper(config = MapStructConfig.class,componentModel = "spring")
public interface ContactInfoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    ContactInfo toEntity(ContactInfoRequest request);

    ContactInfoResponse toDto(ContactInfo entity);
}