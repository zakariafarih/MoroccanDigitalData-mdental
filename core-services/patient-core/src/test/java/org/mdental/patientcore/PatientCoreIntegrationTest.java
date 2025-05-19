package org.mdental.patientcore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mdental.patientcore.model.dto.PatientRequest;
import org.mdental.patientcore.model.entity.Gender;
import org.mdental.patientcore.model.entity.PatientStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class PatientCoreIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private UUID clinicId;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
    }

    @Test
    void createPatient_Success() {
        // Given
        PatientRequest request = PatientRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .gender(Gender.MALE)
                .status(PatientStatus.ACTIVE)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-ClinicId", clinicId.toString());

        // When
        ResponseEntity<?> response = restTemplate.exchange(
                "/api/clinics/{clinicId}/patients",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Object.class,
                clinicId
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getPatient_NotFound() {
        // Given
        UUID nonExistentPatientId = UUID.randomUUID();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-ClinicId", clinicId.toString());

        // When
        ResponseEntity<?> response = restTemplate.exchange(
                "/api/clinics/{clinicId}/patients/{patientId}",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Object.class,
                clinicId,
                nonExistentPatientId
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}