package org.mdental.gatewaycore;

import org.mdental.security.autoconfig.MdentalJwtAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

@SpringBootApplication(
        exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        }
)
@Import(MdentalJwtAutoConfiguration.class)
@EnableDiscoveryClient
public class GatewayCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayCoreApplication.class, args);
    }
}