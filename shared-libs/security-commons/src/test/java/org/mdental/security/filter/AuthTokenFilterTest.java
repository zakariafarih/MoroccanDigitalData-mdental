package org.mdental.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.commons.constants.MdentalHeaders;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.Role;
import org.mdental.security.exception.JwtExceptionHandler;
import org.mdental.security.exception.TokenInvalidException;
import org.mdental.security.jwt.JwtTokenProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTokenFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private JwtExceptionHandler exceptionHandler;

    private AuthTokenFilter filter;
    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID tenantId;
    private String token;
    private AuthPrincipal principal;
    private Set<Role> roles;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        objectMapper = new ObjectMapper();
        filter = new AuthTokenFilter(tokenProvider, objectMapper, exceptionHandler);

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
    }

    @Test
    void doFilterInternal_shouldSetAuthenticationAndHeaders_whenValidToken() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer " + token;
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(Optional.of(token));
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.parseToken(token)).thenReturn(principal);

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")
        );
        when(tokenProvider.getAuthorities(token)).thenReturn(authorities);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);

        // Verify authentication was set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(principal, auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size());

        // Verify headers were propagated
        verify(request).setAttribute(MdentalHeaders.USER_ID, userId.toString());
        verify(request).setAttribute(MdentalHeaders.TENANT_ID, tenantId.toString());
        verify(request).setAttribute(MdentalHeaders.USER_USERNAME, principal.username());
        verify(request).setAttribute(MdentalHeaders.USER_EMAIL, principal.email());
        verify(request).setAttribute(MdentalHeaders.USER_CLINIC_ID, tenantId.toString());
    }

    @Test
    void doFilterInternal_shouldGenerateRequestId_whenMissing() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer " + token;
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(Optional.of(token));
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.parseToken(token)).thenReturn(principal);
        when(tokenProvider.getAuthorities(token)).thenReturn(Collections.emptyList());

        // Missing request ID
        when(request.getHeader(MdentalHeaders.GATEWAY_REQUEST_ID)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(request).setAttribute(eq(MdentalHeaders.GATEWAY_REQUEST_ID), any(String.class));
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenNoAuthHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(tokenProvider, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void filter_shouldRejectMissingAuthorization() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(tokenProvider, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldHandleJwtException() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer " + token;
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(Optional.of(token));

        TokenInvalidException exception = new TokenInvalidException("Invalid token");
        when(tokenProvider.validateToken(token)).thenThrow(exception);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(exceptionHandler).handleJwtException(exception, response);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void filter_shouldPopulateSecurityContext_onValidToken() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer " + token;
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(Optional.of(token));
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.parseToken(token)).thenReturn(principal);

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")
        );
        when(tokenProvider.getAuthorities(token)).thenReturn(authorities);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(principal, auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size());
    }
}