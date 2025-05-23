package org.mdental.gatewaycore.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GatewayConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RouteLocatorBuilder routeLocatorBuilder;

    @Test
    void routeLocatorShouldBeConfigured() {
        GatewayConfig gatewayConfig = new GatewayConfig();
        RouteLocator routeLocator = gatewayConfig.routeLocator(routeLocatorBuilder);

        assertThat(routeLocator).isNotNull();

        // Verify routes are created
        StepVerifier.create(routeLocator.getRoutes())
                .expectNextMatches(r -> r.getId().equals("clinic-core-dev"))
                .expectNextMatches(r -> r.getId().equals("clinic-core-api"))
                .expectNextMatches(r -> r.getId().equals("patient-core-api"))
                .expectNextMatches(r -> r.getId().equals("appointment-core-api"))
                .expectNextMatches(r -> r.getId().equals("auth-core-api"))
                .expectComplete()
                .verify();
    }

}