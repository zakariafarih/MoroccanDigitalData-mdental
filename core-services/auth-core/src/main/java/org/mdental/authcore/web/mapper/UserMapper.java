package org.mdental.authcore.web.mapper;

import org.mapstruct.Mapper;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.web.dto.UserInfoResponse;

/**
 * Mapper for user DTOs.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    /**
     * Map user entity to response DTO.
     *
     * @param user the user entity
     * @return the user response DTO
     */
    UserInfoResponse toUserInfoResponse(User user);
}