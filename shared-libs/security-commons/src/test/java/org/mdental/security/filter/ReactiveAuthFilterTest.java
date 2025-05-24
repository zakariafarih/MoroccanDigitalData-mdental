package org.mdental.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.commons.constants.MdentalHeaders;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.Role;
import org.mdental.security.exception.TokenInvalidException;
import org.mdental.security.jwt.JwtTokenProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveAuthFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private WebFilterChain filterChain;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private HttpHeaders requestHeaders;

    @Mock
    private HttpHeaders responseHeaders;

    @Mock
    private DataBufferFactory dataBufferFactory;

    @Mock
    private DataBuffer dataBuffer;

    private ReactiveAuthFilter filter;
    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID tenantId;
    private String token;
    private AuthPrincipal principal;
    private Set<Role> roles;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        filter = new ReactiveAuthFilter(tokenProvider, objectMapper);

        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        token = "test.jwt.token";
        roles = Set.of(Role.DOCTOR, Role.CLINIC_ADMIN);

        principal = new AuthPrincipal(
                userId,
                tenantId,
                "test.user",
                "test@mdental.org",
                roles
        );

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getHeaders()).thenReturn(requestHeaders);
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.bufferFactory()).thenReturn(dataBufferFactory);
        when(dataBufferFactory.wrap(any(byte[].class))).thenReturn(dataBuffer);
        when(response.writeWith(any())).thenReturn(Mono.empty());
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_shouldContinueChain_whenNoAuthHeader() {
        // Arrange
        when(requestHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(exchange.mutate()).thenReturn(mock(ServerWebExchange.Builder.class));

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(tokenProvider, never()).validateToken(any());
        verify(filterChain).filter(exchange);
    }

    @Test
    void filter_shouldAuthenticateAndMutateRequest_whenValidToken() {
        // Arrange
        String authHeader = "Bearer " + token;
        when(requestHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(Optional.of(token));
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.parseToken(token)).thenReturn(principal);

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")
        );
        when(tokenProvider.getAuthorities(token)).thenReturn(authorities);

        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(mutatedRequest)).thenReturn(exchangeBuilder);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        when(filterChain.filter(mutatedExchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(tokenProvider).validateToken(token);
        verify(tokenProvider).parseToken(token);
        verify(requestBuilder).header(MdentalHeaders.USER_ID, userId.toString());
        verify(requestBuilder).header(MdentalHeaders.TENANT_ID, tenantId.toString());
        verify(requestBuilder).header(MdentalHeaders.USER_USERNAME, principal.username());
        verify(requestBuilder).header(MdentalHeaders.USER_EMAIL, principal.email());
        verify(requestBuilder).header(MdentalHeaders.USER_CLINIC_ID, tenantId.toString());
        verify(filterChain).filter(mutatedExchange);
    }

    @Test
    void filter_shouldGenerateRequestId_whenMissing() {
        // Arrange
        String authHeader = "Bearer " + token;
        when(requestHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(Optional.of(token));
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.parseToken(token)).thenReturn(principal);
        when(tokenProvider.getAuthorities(token)).thenReturn(Collections.emptyList());

        // Missing request ID
        when(requestHeaders.getFirst(MdentalHeaders.GATEWAY_REQUEST_ID)).thenReturn(null);

        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(mutatedRequest)).thenReturn(exchangeBuilder);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        when(filterChain.filter(mutatedExchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(requestBuilder).header(eq(MdentalHeaders.GATEWAY_REQUEST_ID), any(String.class));
    }

    @Test
    void filter_shouldRejectMissingAuthorization() {
        // Arrange
        when(requestHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(tokenProvider, never()).validateToken(any());
        verify(filterChain).filter(exchange);
    }

    @Test
    void filter_shouldHandleJwtException() {
        // Arrange
        String authHeader = "Bearer " + token;
        when(requestHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(Optional.of(token));

        TokenInvalidException exception = new TokenInvalidException("Invalid token");
        when(tokenProvider.validateToken(token)).thenThrow(exception);

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response.getHeaders()).setContentType(any());
        verify(response).writeWith(any());
        verify(filterChain, never()).filter(any());
    }
}