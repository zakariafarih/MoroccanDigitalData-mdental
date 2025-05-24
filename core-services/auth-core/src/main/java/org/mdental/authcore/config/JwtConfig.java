package org.mdental.authcore.config;

import java.time.Clock;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.mdental.authcore.infrastructure.security.KeyGenerator;
import org.mdental.authcore.infrastructure.security.RsaJwtTokenProvider;
import org.mdental.security.autoconfig.JwtProps;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * JWT configuration.
 */
@Configuration
@RequiredArgsConstructor
public class JwtConfig {
    private final KeyGenerator keyGenerator;

    @Value("${mdental.auth.jwt.issuer}")
    private String issuer;

    @Value("${mdental.auth.jwt.access-ttl}")
    private long accessTtl;

    @Value("${mdental.auth.jwt.refresh-ttl}")
    private long refreshTtl;

    @Value("${mdental.auth.token-key-id:auth-core-key}")
    private String kidValue;

    /**
     * List of allowed issuer patterns for JWT validation.
     */
    @Bean(name = "allowedIssuerPatterns")
    public List<String> allowedIssuerPatterns(
            @Value("${mdental.auth.allowed-issuer-patterns}") String patterns
    ) {
        return List.of(patterns.split(","));
    }

    /**
     * The system clock -- used by JwtTokenProvider for timestamps.
     */
    @Bean
    public Clock jwtClock() {
        return Clock.systemUTC();
    }

    /**
     * The JwtProps configured from application properties.
     */
    @Bean
    @Primary
    public JwtProps jwtProps() {
        return new JwtProps(); // will be populated by Spring via @ConfigurationProperties
    }

    /**
     * Custom JwtTokenProvider that uses RSA keys.
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Qualifier("jwtProps") JwtProps jwtProps,
            Clock jwtClock,
            @Autowired(required = false) com.nimbusds.jose.jwk.JWKSet jwkSet
    ) {
        return new RsaJwtTokenProvider(
                jwtProps,
                jwtClock,
                issuer,
                keyGenerator.getPrivateKey(),
                keyGenerator.getPublicKey(),
                accessTtl,
                refreshTtl,
                kidValue,
                jwkSet
        );
    }
}