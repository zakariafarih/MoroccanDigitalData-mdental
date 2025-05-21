package org.mdental.gatewaycore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
        exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.mdental.security.autoconfig.MdentalSecurityAutoConfiguration.class
        }
)

@EnableDiscoveryClient
public class GatewayCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayCoreApplication.class, args);
    }
}
