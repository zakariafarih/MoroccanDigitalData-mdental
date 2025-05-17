package org.mdental.cliniccore.integration;

import org.junit.jupiter.api.Test;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.ClinicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClinicTenantFilteringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClinicRepository clinicRepository;

    @Test
    @Transactional
    @WithMockUser(authorities = {"ROLE_SUPER_ADMIN"})
    public void testCreateAndQueryWithTenantFilter() throws Exception {
        // Create two clinics
        UUID clinic1Id = createClinic("Test Clinic 1", "test-clinic-1", "mdental-test-1");
        UUID clinic2Id = createClinic("Test Clinic 2", "test-clinic-2", "mdental-test-2");

        // Test that super admin can see both clinics
        mockMvc.perform(get("/api/clinics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2));

        // Test with tenant filter for clinic 1
        mockMvc.perform(get("/api/clinics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ClinicId", clinic1Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(clinic1Id.toString()));
    }

    @Test
    @Transactional
    @WithMockUser(username = "clinic-user", authorities = {"ROLE_CLINIC_ADMIN"})
    public void testClinicUserCanOnlyAccessOwnClinic() throws Exception {
        // Create two clinics
        UUID clinic1Id = createClinic("Clinic A", "clinic-a", "mdental-clinic-a");
        UUID clinic2Id = createClinic("Clinic B", "clinic-b", "mdental-clinic-b");

        // Clinic user can access their own clinic
        mockMvc.perform(get("/api/clinics/" + clinic1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ClinicId", clinic1Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(clinic1Id.toString()));

        // Clinic user cannot access another clinic
        mockMvc.perform(get("/api/clinics/" + clinic2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ClinicId", clinic1Id.toString()))
                .andExpect(status().isForbidden());
    }

    private UUID createClinic(String name, String slug, String realm) {
        Clinic clinic = new Clinic();
        clinic.setName(name);
        clinic.setSlug(slug);
        clinic.setRealm(realm);
        clinic.setStatus(Clinic.ClinicStatus.ACTIVE);
        Clinic saved = clinicRepository.save(clinic);
        return saved.getId();
    }
}