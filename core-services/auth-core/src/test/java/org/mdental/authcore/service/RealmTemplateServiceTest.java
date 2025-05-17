package org.mdental.authcore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealmTemplateServiceTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private ClassPathResource resource;

    @InjectMocks
    private RealmTemplateService templateService;

    private String sampleTemplate;

    @BeforeEach
    void setUp() throws IOException {
        sampleTemplate =
                "{\n" +
                        "  \"realm\": \"mdental-__CLINIC_SLUG__\",\n" +
                        "  \"enabled\": true,\n" +
                        "  \"roles\": {\n" +
                        "    \"realm\": [\n" +
                        "      { \"name\": \"__ADMIN_ROLE__\", \"description\": \"Clinic administrator\" },\n" +
                        "      { \"name\": \"DOCTOR\", \"description\": \"Doctor with access\" }\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  \"users\": [\n" +
                        "    {\n" +
                        "      \"username\": \"admin\",\n" +
                        "      \"realmRoles\": [ \"__ADMIN_ROLE__\" ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                sampleTemplate.getBytes(StandardCharsets.UTF_8));

        when(resource.getInputStream()).thenReturn(inputStream);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
    }

    @Test
    void loadAndProcessTemplate_shouldReplaceAllPlaceholders() {
        // Arrange
        Map<String, String> variables = new HashMap<>();
        variables.put("CLINIC_SLUG", "test-clinic");
        variables.put("ADMIN_ROLE", "CLINIC_ADMIN");

        // Act
        String result = templateService.loadAndProcessTemplate(variables);

        // Assert
        assertThat(result).contains("\"realm\": \"mdental-test-clinic\"");
        assertThat(result).contains("\"name\": \"CLINIC_ADMIN\"");
        assertThat(result).contains("\"realmRoles\": [ \"CLINIC_ADMIN\" ]");
        assertThat(result).doesNotContain("__CLINIC_SLUG__");
        assertThat(result).doesNotContain("__ADMIN_ROLE__");
    }

    @Test
    void loadAndProcessTemplate_shouldHandleMissingVariables() {
        // Arrange
        Map<String, String> variables = new HashMap<>();
        variables.put("CLINIC_SLUG", "test-clinic");
        // ADMIN_ROLE is missing

        // Act
        String result = templateService.loadAndProcessTemplate(variables);

        // Assert
        assertThat(result).contains("\"realm\": \"mdental-test-clinic\"");
        assertThat(result).contains("\"name\": \"\"");
        assertThat(result).contains("\"realmRoles\": [ \"\" ]");
        assertThat(result).doesNotContain("__CLINIC_SLUG__");
        assertThat(result).doesNotContain("__ADMIN_ROLE__");
    }
}