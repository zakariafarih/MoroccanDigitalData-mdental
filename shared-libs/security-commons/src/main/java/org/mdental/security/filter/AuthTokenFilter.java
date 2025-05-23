package org.mdental.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.constants.MdentalHeaders;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.security.exception.JwtExceptionHandler;
import org.mdental.security.jwt.JwtException;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**

 Filter that extracts and validates JWT tokens for servlet-based applications (MVC)
 */
@Slf4j
@RequiredArgsConstructor
@Schema(description = "JWT authentication filter for MVC applications")
public class AuthTokenFilter extends OncePerRequestFilter {

    @NonNull
    private final JwtTokenProvider tokenProvider;

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final JwtExceptionHandler exceptionHandler;

    private final Clock clock;

    /**

     Constructor with clock for testing time-based operations
     */
    public AuthTokenFilter(JwtTokenProvider tokenProvider, ObjectMapper objectMapper,
                           JwtExceptionHandler exceptionHandler) {
        this(tokenProvider, objectMapper, exceptionHandler, Clock.systemUTC());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            Optional<String> tokenOpt = tokenProvider.extractTokenFromHeader(authHeader);

            if (tokenOpt.isPresent()) {
                String token = tokenOpt.get();

                // Validate token
                if (tokenProvider.validateToken(token)) {
                    AuthPrincipal principal = tokenProvider.parseToken(token);

                    // Create Spring Security authentication
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            tokenProvider.getAuthorities(token)
                    );

                    // Set in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Propagate headers – both to the request scope *and*
                    // as actual HTTP headers for downstream services
                    propagateHeaders(request, response, principal);
                }
            }

            filterChain.doFilter(request, response);

        } catch (JwtException ex) {
            exceptionHandler.handleJwtException(ex, response);
        }
    }

    /**

     Propagate user information as HTTP headers for downstream services
     */
    private void propagateHeaders(HttpServletRequest request,
                                  HttpServletResponse response,
                                  AuthPrincipal principal) {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(response, "Response cannot be null");
        Objects.requireNonNull(principal, "Principal cannot be null");

// Add user ID
        request.setAttribute(MdentalHeaders.USER_ID, principal.id().toString());


// Add tenant ID
        request.setAttribute(MdentalHeaders.TENANT_ID, principal.tenantId().toString());

// Add username
        request.setAttribute(MdentalHeaders.USER_USERNAME, principal.username());

// Add email
        request.setAttribute(MdentalHeaders.USER_EMAIL, principal.email());

// Set clinic ID (same as tenant for now)
        request.setAttribute(MdentalHeaders.USER_CLINIC_ID, principal.tenantId().toString());

// Generate request ID if not present
        String requestId = request.getHeader(MdentalHeaders.GATEWAY_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            request.setAttribute(MdentalHeaders.GATEWAY_REQUEST_ID, requestId);
        }

        // === outbound HTTP headers ===
        response.setHeader(MdentalHeaders.USER_ID, principal.id().toString());
        response.setHeader(MdentalHeaders.TENANT_ID, principal.tenantId().toString());
        response.setHeader(MdentalHeaders.USER_USERNAME, principal.username());
        response.setHeader(MdentalHeaders.USER_EMAIL, principal.email());
        response.setHeader(MdentalHeaders.USER_CLINIC_ID, principal.tenantId().toString());
        response.setHeader(MdentalHeaders.GATEWAY_REQUEST_ID, requestId);
    }
}