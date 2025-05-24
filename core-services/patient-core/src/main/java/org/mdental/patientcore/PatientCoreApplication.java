package org.mdental.patientcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PatientCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientCoreApplication.class, args);
    }
}