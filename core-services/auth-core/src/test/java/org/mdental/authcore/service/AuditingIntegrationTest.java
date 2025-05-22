package org.mdental.authcore.service;

import org.junit.jupiter.api.Test;
import org.mdental.authcore.model.entity.Realm;
import org.mdental.authcore.repository.RealmRepository;
import org.mdental.commons.constants.MdentalHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RealmRepository realmRepository;

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void shouldPopulateAuditFields() throws Exception {
        // Arrange
        final String testUsername = "test-user";

        // Create a test realm directly in the repo to test auditing
        Realm realm = Realm.builder()
                .name("mdental-test-" + System.currentTimeMillis())
                .clinicSlug("test-" + System.currentTimeMillis())
                .issuer("http://localhost:9080/realms/test")
                .adminUsername("admin")
                .build();

        // Act - Make a request with X-User-Username header
        mockMvc.perform(post("/realms")
                        .header(MdentalHeaders.USER_USERNAME, testUsername)
                        .contentType("application/json")
                        .content("{\"realm\": \"" + realm.getName() + "\", \"clinicSlug\": \"" + realm.getClinicSlug() + "\"}"))
                .andExpect(status().isOk());

        // Assert - Find the newly created realm and check audit fields
        Realm savedRealm = realmRepository.findByName(realm.getName()).orElseThrow();

        assertThat(savedRealm.getCreatedBy()).isEqualTo(testUsername);
        assertThat(savedRealm.getCreatedAt()).isNotNull();
    }
}