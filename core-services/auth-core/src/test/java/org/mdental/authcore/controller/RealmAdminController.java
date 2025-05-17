package org.mdental.authcore.controller;

import org.junit.jupiter.api.Test;
import org.mdental.authcore.api.dto.CreateRealmRequest;
import org.mdental.authcore.api.dto.RealmResponse;
import org.mdental.authcore.api.dto.RegeneratePasswordRequest;
import org.mdental.authcore.service.RealmProvisioningService;
import org.mdental.commons.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RealmAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RealmProvisioningService realmProvisioningService;

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void createRealm_success() throws Exception {
        // Setup
        CreateRealmRequest request = new CreateRealmRequest();
        request.setRealm("mdental-test");
        request.setClinicSlug("test");

        RealmResponse mockResponse = RealmResponse.builder()
                .realmName("mdental-test")
                .issuer("http://localhost:8080/realms/mdental-test")
                .kcRealmAdminUser("admin")
                .tmpPassword("password123")
                .build();

        when(realmProvisioningService.createRealm(eq("mdental-test"), eq("test")))
                .thenReturn(mockResponse);

        // Test
        mockMvc.perform(post("/realms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"realm\": \"mdental-test\", \"clinicSlug\": \"test\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.realmName").value("mdental-test"))
                .andExpect(jsonPath("$.data.issuer").value("http://localhost:8080/realms/mdental-test"))
                .andExpect(jsonPath("$.data.kcRealmAdminUser").value("admin"))
                .andExpect(jsonPath("$.data.tmpPassword").value("password123"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void regenerateAdminPassword_success() throws Exception {
        // Setup
        RegeneratePasswordRequest request = new RegeneratePasswordRequest();
        request.setAdminUsername("admin");

        RealmResponse mockResponse = RealmResponse.builder()
                .realmName("mdental-test")
                .issuer("http://localhost:8080/realms/mdental-test")
                .kcRealmAdminUser("admin")
                .tmpPassword("newpassword123")
                .build();

        when(realmProvisioningService.regenerateAdminPassword(eq("mdental-test"), eq("admin")))
                .thenReturn(mockResponse);

        // Test
        mockMvc.perform(post("/realms/mdental-test/regenerate-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"adminUsername\": \"admin\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.realmName").value("mdental-test"))
                .andExpect(jsonPath("$.data.tmpPassword").value("newpassword123"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLINIC_ADMIN") // not a SUPER_ADMIN
    void createRealm_forbidden() throws Exception {
        // Test
        mockMvc.perform(post("/realms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"realm\": \"mdental-test\", \"clinicSlug\": \"test\" }"))
                .andExpect(status().isForbidden());
    }
}
