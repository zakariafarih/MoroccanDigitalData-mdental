package org.mdental.authcore.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthCoreSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user", roles = "CLINIC_ADMIN")
    void apiRequestsShouldNotRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/test")
                        .content("{}")
                        .contentType("application/json"))
                .andExpect(status().isNotFound()); // 404 expected since endpoint doesn't exist, but not 403 Forbidden
    }

    @Test
    @WithMockUser(username = "user", roles = "CLINIC_ADMIN")
    void swaggerUiRequestsShouldRequireCsrf() throws Exception {
        // POST to swagger UI should fail without CSRF
        mockMvc.perform(post("/swagger-ui/test")
                        .content("{}")
                        .contentType("application/json"))
                .andExpect(status().isForbidden()); // 403 Forbidden expected due to CSRF protection
    }

    @Test
    @WithMockUser(username = "user", roles = "CLINIC_ADMIN")
    void swaggerUiWithCsrfShouldSucceed() throws Exception {
        // GET requests are not affected by CSRF
        mockMvc.perform(get("/swagger-ui/index.html")
                        .with(csrf()))
                .andExpect(status().isOk());

        // POST with CSRF token should pass CSRF check (but may still fail for other reasons)
        mockMvc.perform(post("/swagger-ui/test")
                        .with(csrf())
                        .content("{}")
                        .contentType("application/json"))
                .andExpect(status().isNotFound()); // 404 expected since endpoint doesn't exist, but not 403
    }
}