package org.mdental.cliniccore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mdental.cliniccore.mapper.ClinicMapper;
import org.mdental.cliniccore.model.dto.ClinicResponse;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.dto.UpdateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.service.ClinicProvisioningService;
import org.mdental.cliniccore.service.ClinicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClinicController.class)
public class ClinicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClinicService clinicService;

    @MockBean
    private ClinicMapper clinicMapper;

    @MockBean
    private ClinicProvisioningService clinicProvisioningService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID clinicId;
    private Clinic clinic;
    private ClinicResponse clinicResponse;
    private CreateClinicRequest createRequest;
    private UpdateClinicRequest updateRequest;

    @BeforeEach
    void setUp() {
        clinicId = UUID.fromString("69d74c65-ee91-421a-8181-9894a6377b2f");

        // Create a clinic entity
        clinic = new Clinic();
        clinic.setId(clinicId);
        clinic.setName("Test Clinic");
        clinic.setSlug("test-clinic");
        clinic.setRealm("test-clinic");
        clinic.setStatus(Clinic.ClinicStatus.ACTIVE);
        clinic.setContactInfos(new HashSet<>());
        clinic.setAddresses(new HashSet<>());
        clinic.setBusinessHours(new HashSet<>());
        clinic.setHolidays(new HashSet<>());
        clinic.setCreatedAt(Instant.now());
        clinic.setCreatedBy("test-user");

        // Create clinic response DTO
        clinicResponse = ClinicResponse.builder()
                .id(clinicId)
                .name("Test Clinic")
                .slug("test-clinic")
                .realm("test-clinic")
                .status(Clinic.ClinicStatus.ACTIVE)
                .createdAt(clinic.getCreatedAt())
                .createdBy("test-user")
                .build();

        createRequest = CreateClinicRequest.builder()
                .name("New Clinic")
                .slug("new-clinic")
                .realm("new-clinic")
                .build();

        updateRequest = UpdateClinicRequest.builder()
                .name("Updated Clinic")
                .build();

        // Set up mapper
        when(clinicMapper.toDto(any(Clinic.class))).thenReturn(clinicResponse);

        // provisioning stub used by POST /api/clinics
        when(clinicProvisioningService.provisionClinic(any(CreateClinicRequest.class)))
                .thenReturn(clinic);
    }

    @Test
    void createClinic_shouldReturnCreatedClinic() throws Exception {
        // Arrange
        when(clinicProvisioningService.provisionClinic(any(CreateClinicRequest.class)))
                .thenReturn(clinic);

        // Act & Assert
        mockMvc.perform(post("/api/clinics")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .header("X-User-Username", "test-admin")
                        .header("X-User-ClinicId", clinicId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(clinicId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Clinic"));
    }

    @Test
    void getClinic_shouldReturnClinic() throws Exception {
        // Arrange
        when(clinicService.getClinicById(clinicId)).thenReturn(clinic);

        // Act & Assert
        mockMvc.perform(get("/api/clinics/{id}", clinicId)
                        .header("X-User-Username", "test-admin")
                        .header("X-User-ClinicId", clinicId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(clinicId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Clinic"));
    }

    @Test
    void updateClinic_shouldReturnUpdatedClinic() throws Exception {
        // Arrange
        when(clinicService.updateClinic(eq(clinicId), any(UpdateClinicRequest.class))).thenReturn(clinic);
        when(clinicService.getClinicById(clinicId)).thenReturn(clinic);

        // Act & Assert
        mockMvc.perform(put("/api/clinics/{id}", clinicId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Username", "test-admin")
                        .header("X-User-ClinicId", clinicId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(clinicId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Clinic"));
    }

    @Test
    void updateClinicStatus_shouldReturnUpdatedClinic() throws Exception {
        // Arrange
        when(clinicService.updateClinicStatus(eq(clinicId), any(Clinic.ClinicStatus.class))).thenReturn(clinic);
        when(clinicService.getClinicById(clinicId)).thenReturn(clinic);

        // Act & Assert
        mockMvc.perform(patch("/api/clinics/{id}/status", clinicId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("status", "INACTIVE")
                        .header("X-User-Username", "test-admin")
                        .header("X-User-ClinicId", clinicId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(clinicId.toString()));
    }

    @Test
    void deleteClinic_shouldReturnNoContent() throws Exception {
        // Arrange
        when(clinicService.getClinicById(clinicId)).thenReturn(clinic);

        // Act & Assert
        mockMvc.perform(delete("/api/clinics/{id}", clinicId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("X-User-Username", "test-admin")
                        .header("X-User-ClinicId", clinicId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllClinics_shouldReturnPaginatedClinics() throws Exception {
        // Arrange
        PageImpl<Clinic> page = new PageImpl<>(List.of(clinic));
        when(clinicService.getFilteredClinics(any(), any(), any(Pageable.class))).thenReturn(page);
        when(clinicService.getClinicById(any())).thenReturn(clinic);

        // Act & Assert
        mockMvc.perform(get("/api/clinics")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-User-Username", "test-admin")
                        .header("X-User-ClinicId", clinicId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(clinicId.toString()));
    }
}