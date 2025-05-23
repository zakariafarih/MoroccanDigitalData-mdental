package org.mdental.authcore.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.application.service.OAuthService;
import org.mdental.authcore.exception.AuthenticationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for OAuth 2.1 compatible endpoints.
 */
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {
    private final OAuthService oauthService;

    /**
     * OAuth token endpoint.
     *
     * @param grantType the grant type
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param username the username (for password grant)
     * @param password the password (for password grant)
     * @param refreshToken the refresh token (for refresh_token grant)
     * @param request the HTTP request
     * @return token response
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request) {

        log.debug("OAuth token request: grant_type={}, client_id={}", grantType, clientId);

        try {
            String ipAddress = getClientIp(request);

            Map<String, Object> response = oauthService.processTokenRequest(
                    grantType,
                    clientId,
                    clientSecret,
                    username,
                    password,
                    refreshToken,
                    ipAddress
            );

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            log.warn("OAuth authentication error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_grant",
                    "error_description", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("OAuth token error", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "server_error",
                    "error_description", "An error occurred processing the token request"
            ));
        }
    }

    /**
     * OAuth token introspection endpoint.
     *
     * @param token the token to introspect
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @return introspection response
     */
    @PostMapping(value = "/introspect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> introspect(
            @RequestParam("token") String token,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret) {

        log.debug("OAuth token introspection request");

        try {
            // Implement client validation if required

            // Introspect token
            Map<String, Object> response = oauthService.introspectToken(token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OAuth introspection error", e);
            return ResponseEntity.ok(Map.of("active", false));
        }
    }

    /**
     * Extract client IP address from the request.
     *
     * @param request the HTTP request
     * @return the client IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}