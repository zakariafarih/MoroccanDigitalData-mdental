package org.mdental.authcore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mdental.authcore.api.dto.LoginRequest;
import org.mdental.authcore.api.dto.RecoverRequest;
import org.mdental.authcore.api.dto.RefreshRequest;
import org.mdental.authcore.api.dto.RegisterRequest;
import org.mdental.authcore.api.dto.UserInfoResponse;
import org.mdental.authcore.exception.NotFoundException;
import org.mdental.authcore.service.TenantAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantAuthService tenantAuthService;

    @Test
    void login_success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("user", "password");
        Map<String, Object> mockResponse = Map.of(
                "access_token", "eyJ...",
                "refresh_token", "eyJ...",
                "expires_in", 300
        );

        when(tenantAuthService.login(eq("test"), eq("user"), eq("password"))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("eyJ..."))
                .andExpect(jsonPath("$.refresh_token").value("eyJ..."))
                .andExpect(jsonPath("$.expires_in").value(300));
    }

    @Test
    void login_invalidData() throws Exception {
        // Arrange - missing required fields
        LoginRequest request = new LoginRequest("", "");

        // Act & Assert
        mockMvc.perform(post("/auth/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_tenantNotFound() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("user", "password");
        when(tenantAuthService.login(eq("nonexistent"), eq("user"), eq("password")))
                .thenThrow(new NotFoundException("Tenant not found: nonexistent"));

        // Act & Assert
        mockMvc.perform(post("/auth/nonexistent/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void refresh_success() throws Exception {
        // Arrange
        RefreshRequest request = new RefreshRequest("eyJ...");
        Map<String, Object> mockResponse = Map.of(
                "access_token", "newEyJ...",
                "refresh_token", "newEyJ...",
                "expires_in", 300
        );

        when(tenantAuthService.refreshToken(eq("test"), eq("eyJ..."))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/test/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("newEyJ..."))
                .andExpect(jsonPath("$.refresh_token").value("newEyJ..."))
                .andExpect(jsonPath("$.expires_in").value(300));
    }

    @Test
    void register_success() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .firstName("New")
                .lastName("User")
                .password("password123")
                .role("DOCTOR")
                .build();

        doNothing().when(tenantAuthService).registerUser(
                eq("test"),
                eq("newuser"),
                eq("new@example.com"),
                eq("New"),
                eq("User"),
                eq("password123"),
                eq("DOCTOR")
        );

        // Act & Assert
        mockMvc.perform(post("/auth/test/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tenantAuthService).registerUser(
                eq("test"),
                eq("newuser"),
                eq("new@example.com"),
                eq("New"),
                eq("User"),
                eq("password123"),
                eq("DOCTOR")
        );
    }

    @Test
    void register_defaultRole() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .firstName("New")
                .lastName("User")
                .password("password123")
                // No role specified - should default to PATIENT
                .build();

        doNothing().when(tenantAuthService).registerUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        );

        // Act & Assert
        mockMvc.perform(post("/auth/test/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(tenantAuthService).registerUser(
                eq("test"),
                eq("newuser"),
                eq("new@example.com"),
                eq("New"),
                eq("User"),
                eq("password123"),
                isNull()
        );
    }

    @Test
    void recover_alwaysReturns204() throws Exception {
        // Arrange
        RecoverRequest request = new RecoverRequest("user@example.com");

        // Even if service throws exception, controller should still return 204
        doThrow(new RuntimeException("Email not found"))
                .when(tenantAuthService).triggerPasswordReset(eq("test"), eq("user@example.com"));

        // Act & Assert - Should still return 204
        mockMvc.perform(post("/auth/test/recover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void me_success() throws Exception {
        // Arrange
        UserInfoResponse mockResponse = UserInfoResponse.builder()
                .sub("user-123")
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .realm("mdental-test")
                .roles(Set.of("PATIENT", "USER"))
                .build();

        when(tenantAuthService.getCurrentUser(any())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/auth/test/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Test"))
                .andExpect(jsonPath("$.data.lastName").value("User"))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[0]").exists());
    }
}