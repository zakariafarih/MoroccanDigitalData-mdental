package org.mdental.cliniccore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ClinicCoreApplicationTests {

    @Test
    void contextLoads() {
        // If this runs without exception, the Spring context loads successfully
    }
}