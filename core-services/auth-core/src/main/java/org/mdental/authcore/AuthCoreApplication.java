package org.mdental.authcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Auth Core service.
 */
@SpringBootApplication(
        scanBasePackages = {
                "org.mdental.authcore",
                "org.mdental.security.jwt"
        }
)
@EnableScheduling
public class AuthCoreApplication {
    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthCoreApplication.class, args);
    }
}