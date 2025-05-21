package org.mdental.patientcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mdental.patientcore.model.dto.ConsentRequest;
import org.mdental.patientcore.model.dto.ConsentResponse;
import org.mdental.patientcore.model.entity.Consent;

@Mapper(config = MapStructConfig.class)
public interface ConsentMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    Consent toEntity(ConsentRequest request);

    ConsentResponse toDto(Consent entity);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version",   ignore = true)
    void updateEntityFromDto(ConsentRequest request, @MappingTarget Consent entity);
}
