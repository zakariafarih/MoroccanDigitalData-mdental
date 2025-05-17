package org.mdental.authcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.mdental.authcore.api.dto.UserInfoResponse;
import org.mdental.commons.model.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "User Info", description = "User information operations")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns information about the authenticated user based on JWT")
    public ApiResponse<UserInfoResponse> me(@AuthenticationPrincipal Jwt jwt) {

        String realm = jwt.getIssuer().getPath();           // "…/realms/<realm>"
        realm = realm.substring(realm.lastIndexOf('/') + 1);

        Set<String> roles = Collections.emptySet();
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof java.util.Collection<?> raw) {
            roles = raw.stream().map(Object::toString).collect(Collectors.toSet());
        }

        return ApiResponse.success(
                UserInfoResponse.builder()
                        .sub(jwt.getSubject())
                        .username(jwt.getClaimAsString("preferred_username"))
                        .email(jwt.getClaimAsString("email"))
                        .firstName(jwt.getClaimAsString("given_name"))
                        .lastName(jwt.getClaimAsString("family_name"))
                        .realm(realm)
                        .roles(roles)
                        .build()
        );
    }
}
