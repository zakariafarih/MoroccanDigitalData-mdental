package org.mdental.authcore.web.mapper;

import org.mapstruct.Mapper;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.web.dto.UserInfoResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for user DTOs.
 */
@Component
public class UserMapperImpl implements UserMapper {
    @Override
    public UserInfoResponse toUserInfoResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserInfoResponse(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEmailVerified(),
                user.getRoles(),
                user.getLastLoginAt()
        );
    }
}