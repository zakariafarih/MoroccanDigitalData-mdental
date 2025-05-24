package org.mdental.cliniccore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.cliniccore.mapper.ClinicMapper;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.dto.UpdateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.ClinicRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClinicServiceTest {

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private ClinicMapper clinicMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private ClinicService clinicService;

    private UUID clinicId;
    private Clinic clinic;
    private CreateClinicRequest createRequest;
    private UpdateClinicRequest updateRequest;

    @BeforeEach
    void setUp() {
        clinicId = UUID.fromString("98738f27-eacb-4de0-a2bc-d4efa1c47547");

        // Create a clinic for testing
        clinic = new Clinic();
        clinic.setId(clinicId);
        clinic.setName("Test Clinic");
        clinic.setSlug("test-clinic");
        clinic.setRealm("test-clinic");
        clinic.setStatus(Clinic.ClinicStatus.ACTIVE);
        clinic.setCreatedAt(Instant.now());
        clinic.setCreatedBy("test-user");

        createRequest = CreateClinicRequest.builder()
                .name("New Clinic")
                .slug("new-clinic")
                .realm("new-clinic")
                .build();

        updateRequest = UpdateClinicRequest.builder()
                .name("Updated Clinic")
                .build();

        when(clinicMapper.toEntity(any(CreateClinicRequest.class))).thenReturn(clinic);
    }

    @Test
    void getAllClinics_shouldReturnAllClinics() {
        // Arrange
        List<Clinic> clinics = Arrays.asList(clinic);
        when(clinicRepository.findAll()).thenReturn(clinics);

        // Act
        List<Clinic> result = clinicService.getAllClinics();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Clinic");
        verify(clinicRepository, times(1)).findAll();
    }

    @Test
    void getClinicById_whenClinicExists_shouldReturnClinic() {
        // Arrange
        when(clinicRepository.findByIdWithAllRelationships(clinicId)).thenReturn(Optional.of(clinic));

        // Act
        Clinic result = clinicService.getClinicById(clinicId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(clinicId);
        verify(clinicRepository, times(1)).findByIdWithAllRelationships(clinicId);
    }

    @Test
    void getClinicById_whenClinicDoesNotExist_shouldThrowException() {
        // Arrange
        when(clinicRepository.findByIdWithAllRelationships(clinicId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ClinicService.ClinicNotFoundException.class, () -> {
            clinicService.getClinicById(clinicId);
        });
        verify(clinicRepository, times(1)).findByIdWithAllRelationships(clinicId);
    }

    @Test
    void createClinic_whenRealmIsUnique_shouldCreateClinic() {
        // Arrange
        when(clinicRepository.findByRealm(createRequest.getRealm())).thenReturn(Optional.empty());
        when(clinicRepository.findBySlug(createRequest.getSlug())).thenReturn(Optional.empty());
        when(clinicRepository.save(any(Clinic.class))).thenReturn(clinic);

        // Act
        Clinic result = clinicService.createClinic(createRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(clinicRepository, times(1)).findByRealm(createRequest.getRealm());
        verify(clinicRepository, times(1)).findBySlug(createRequest.getSlug());
        verify(clinicRepository, times(1)).save(any(Clinic.class));
        verify(publisher, times(1)).publishEvent(any());
    }

    @Test
    void createClinic_whenRealmExists_shouldThrowException() {
        // Arrange
        when(clinicRepository.findByRealm(createRequest.getRealm())).thenReturn(Optional.of(clinic));

        // Act & Assert
        assertThrows(ClinicService.ClinicAlreadyExistsException.class, () -> {
            clinicService.createClinic(createRequest);
        });
        verify(clinicRepository, times(1)).findByRealm(createRequest.getRealm());
        verify(clinicRepository, never()).save(any(Clinic.class));
    }

    @Test
    void updateClinic_whenClinicExists_shouldUpdateClinic() {
        // Arrange
        when(clinicRepository.findByIdWithAllRelationships(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicRepository.save(any(Clinic.class))).thenReturn(clinic);

        // Act
        Clinic result = clinicService.updateClinic(clinicId, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(clinicRepository, times(1)).findByIdWithAllRelationships(clinicId);
        verify(clinicRepository, times(1)).save(any(Clinic.class));
        verify(publisher, times(1)).publishEvent(any());
    }

    @Test
    void updateClinicStatus_whenClinicExists_shouldUpdateStatus() {
        // Arrange
        when(clinicRepository.findByIdWithAllRelationships(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicRepository.save(any(Clinic.class))).thenReturn(clinic);

        // Act
        Clinic result = clinicService.updateClinicStatus(clinicId, Clinic.ClinicStatus.INACTIVE);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Clinic.ClinicStatus.INACTIVE);
        verify(clinicRepository, times(1)).findByIdWithAllRelationships(clinicId);
        verify(clinicRepository, times(1)).save(any(Clinic.class));
        verify(publisher, times(1)).publishEvent(any());
    }

    @Test
    void deleteClinic_whenClinicExists_shouldDeleteClinic() {
        // Arrange
        when(clinicRepository.findByIdWithAllRelationships(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicRepository.save(any(Clinic.class))).thenReturn(clinic);

        // Act
        clinicService.deleteClinic(clinicId);

        // Assert
        verify(clinicRepository, times(1)).findByIdWithAllRelationships(clinicId);

        // Verify soft delete - this is key to the fix
        ArgumentCaptor<Clinic> clinicCaptor = ArgumentCaptor.forClass(Clinic.class);
        verify(clinicRepository, times(1)).save(clinicCaptor.capture());

        Clinic capturedClinic = clinicCaptor.getValue();
        assertThat(capturedClinic.getDeletedAt()).isNotNull();
        assertThat(capturedClinic.getDeletedBy()).isNotNull();

        verify(publisher, times(1)).publishEvent(any());
    }

    @Test
    void getFilteredClinics_withRealm_shouldFilterByRealm() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Clinic> page = new PageImpl<>(List.of(clinic));
        when(clinicRepository.findByRealmAndNameContainingIgnoreCase(eq("test-realm"), eq(""), eq(pageable)))
                .thenReturn(page);

        // Act
        Page<Clinic> result = clinicService.getFilteredClinics("test-realm", "", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(clinicRepository, times(1))
                .findByRealmAndNameContainingIgnoreCase(eq("test-realm"), eq(""), eq(pageable));
    }

    @Test
    void getFilteredClinics_withoutRealm_shouldNotFilterByRealm() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Clinic> page = new PageImpl<>(List.of(clinic));
        when(clinicRepository.findByNameContainingIgnoreCase(eq(""), eq(pageable)))
                .thenReturn(page);

        // Act
        Page<Clinic> result = clinicService.getFilteredClinics(null, "", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(clinicRepository, times(1))
                .findByNameContainingIgnoreCase(eq(""), eq(pageable));
    }
}