package org.mdental.gatewaycore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayCoreApplication.class, args);
    }
}