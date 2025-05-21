package org.mdental.patientcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mdental.patientcore.model.dto.AddressRequest;
import org.mdental.patientcore.model.dto.AddressResponse;
import org.mdental.patientcore.model.entity.Address;

@Mapper(config = MapStructConfig.class)
public interface AddressMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    Address toEntity(AddressRequest request);

    AddressResponse toDto(Address entity);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    void updateEntityFromDto(AddressRequest request, @MappingTarget Address entity);
}
