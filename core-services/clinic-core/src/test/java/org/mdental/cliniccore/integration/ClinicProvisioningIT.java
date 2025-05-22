package org.mdental.cliniccore.integration;

import org.junit.jupiter.api.Test;
import org.mdental.authcore.api.dto.RealmResponse;
import org.mdental.cliniccore.client.AuthCoreClient;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.ClinicRepository;
import org.mdental.commons.model.ApiResponse;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class ClinicProvisioningIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClinicRepository clinicRepository;

    @MockBean
    private AuthCoreClient authCoreClient;

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    private static final GenericContainer<?> keycloakContainer = new GenericContainer<>("quay.io/keycloak/keycloak:19.0.1")
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withEnv("KC_FEATURES", "token-exchange")
            .withCommand("start-dev")
            .waitingFor(Wait.forHttp("/realms/master").forStatusCode(200));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        registry.add("keycloak.auth.url", () -> "http://localhost:" + keycloakContainer.getMappedPort(8080) + "/realms/master/protocol/openid-connect/token");
        registry.add("keycloak.admin.url", () -> "http://localhost:" + keycloakContainer.getMappedPort(8080) + "/admin/realms");
    }

    @Test
    @Transactional
    void testCreateClinicWithRealm() {
        // Arrange
        // Mock the auth-core client response
        RealmResponse realmResponse = new RealmResponse();
        realmResponse.setRealmName("mdental-test-clinic");
        realmResponse.setIssuer("http://localhost:9080/realms/mdental-test-clinic");
        realmResponse.setKcRealmAdminUser("admin");
        realmResponse.setTmpPassword("tempPwd123");

        ApiResponse<RealmResponse> apiResponse =
                ApiResponse.success(realmResponse);

        when(authCoreClient.createRealm(any())).thenReturn(apiResponse);

        // Create a unique clinic for this test
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        CreateClinicRequest request = CreateClinicRequest.builder()
                .name("Test Clinic " + uniqueId)
                .slug("test-clinic-" + uniqueId)
                .realm("mdental-test-" + uniqueId)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CreateClinicRequest> entity = new HttpEntity<>(request, headers);

        // Act
        // Call the clinic creation endpoint
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/clinics",
                HttpMethod.POST,
                entity,
                ApiResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        // Verify Keycloak realm creation was requested
        Mockito.verify(authCoreClient).createRealm(any());

        // Verify clinic was saved with Keycloak info
        Clinic savedClinic = clinicRepository.findByRealm(request.getRealm()).orElse(null);
        assertThat(savedClinic).isNotNull();
        assertThat(savedClinic.getKcIssuer()).isEqualTo(realmResponse.getIssuer());
        assertThat(savedClinic.getKcAdminUser()).isEqualTo(realmResponse.getKcRealmAdminUser());
        assertThat(savedClinic.getKcTmpPassword()).isEqualTo(realmResponse.getTmpPassword());
    }

    @Test
    void testClinicCreationFailsIfRealmProvisioningFails() {
        // Arrange - mock a failure in the auth-core client
        when(authCoreClient.createRealm(any())).thenThrow(new RuntimeException("Realm creation failed"));

        // Create request
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        CreateClinicRequest request = CreateClinicRequest.builder()
                .name("Failed Clinic " + uniqueId)
                .slug("failed-clinic-" + uniqueId)
                .realm("mdental-failed-" + uniqueId)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateClinicRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/clinics",
                HttpMethod.POST,
                entity,
                ApiResponse.class
        );

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();

        // Verify clinic was NOT saved to database (transaction rollback worked)
        Clinic savedClinic = clinicRepository.findByRealm(request.getRealm()).orElse(null);
        assertThat(savedClinic).isNull();
    }
}