package org.mdental.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.constants.MdentalHeaders;
import org.mdental.commons.model.ApiError;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.ErrorCode;
import org.mdental.security.jwt.JwtException;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**

 Filter that extracts and validates JWT tokens for reactive applications (WebFlux)
 */
@Slf4j
@RequiredArgsConstructor
@Schema(description = "JWT authentication filter for reactive applications")
public class ReactiveAuthFilter implements WebFilter {

    @NonNull
    private final JwtTokenProvider tokenProvider;

    @NonNull
    private final ObjectMapper objectMapper;

    private final Clock clock;

    /**

     Constructor with default system clock
     */
    public ReactiveAuthFilter(JwtTokenProvider tokenProvider, ObjectMapper objectMapper) {
        this(tokenProvider, objectMapper, Clock.systemUTC());
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Objects.requireNonNull(exchange, "Exchange cannot be null");
        Objects.requireNonNull(chain, "Filter chain cannot be null");
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Extract authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        Optional<String> tokenOpt = tokenProvider.extractTokenFromHeader(authHeader);

        if (tokenOpt.isEmpty()) {
            return chain.filter(exchange);
        }

        String token = tokenOpt.get();

        try {
            // Validate token
            if (tokenProvider.validateToken(token)) {
                AuthPrincipal principal = tokenProvider.parseToken(token);

                // Create Spring Security authentication
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        tokenProvider.getAuthorities(token)
                );

                // Propagate headers to the request
                ServerHttpRequest mutatedRequest = propagateHeaders(request, principal);

                // Create a new exchange with the mutated request
                ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                // Set security context and continue the filter chain
                return chain.filter(mutatedExchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            }

            return chain.filter(exchange);

        } catch (JwtException ex) {
            return handleJwtException(ex, response);
        }
    }

    /**

     Handle JWT exceptions by returning appropriate error responses
     */
    private Mono<Void> handleJwtException(JwtException ex, ServerHttpResponse response) {
        ErrorCode errorCode;

        if (ex instanceof org.mdental.security.exception.TokenExpiredException) {
            errorCode = ErrorCode.JWT_EXPIRED;
        } else if (ex instanceof org.mdental.security.exception.TokenSignatureInvalidException) {
            errorCode = ErrorCode.JWT_SIGNATURE_INVALID;
        } else {
            errorCode = ErrorCode.JWT_INVALID;
        }

        log.debug("JWT error: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .code(errorCode)
                .message(ex.getMessage())
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.error(error);

        response.setStatusCode(errorCode.toHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error writing JWT error response", e);
            return Mono.error(e);
        }
    }

    /**

     Propagate user information as HTTP headers for downstream services
     */
    private ServerHttpRequest propagateHeaders(ServerHttpRequest request, AuthPrincipal principal) {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(principal, "Principal cannot be null");

        return request.mutate()
                .header(MdentalHeaders.USER_ID, principal.id().toString())
                .header(MdentalHeaders.TENANT_ID, principal.tenantId().toString())
                .header(MdentalHeaders.USER_USERNAME, principal.username())
                .header(MdentalHeaders.USER_EMAIL, principal.email())
                .header(MdentalHeaders.USER_CLINIC_ID, principal.tenantId().toString())
                .header(MdentalHeaders.GATEWAY_REQUEST_ID,
                        Optional.ofNullable(request.getHeaders().getFirst(MdentalHeaders.GATEWAY_REQUEST_ID))
                                .orElse(UUID.randomUUID().toString()))
                .build();
    }
}