package org.mdental.authcore.controller;

import lombok.RequiredArgsConstructor;
import org.mdental.authcore.model.dto.UserInfoResponse;
import org.mdental.commons.model.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(@AuthenticationPrincipal Jwt jwt) {

        String realm = jwt.getIssuer().getPath();           // “…/realms/<realm>”
        realm = realm.substring(realm.lastIndexOf('/') + 1);

        java.util.Set<String> roles = java.util.Collections.emptySet();
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof java.util.Collection<?> raw) {
            roles = raw.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet());
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