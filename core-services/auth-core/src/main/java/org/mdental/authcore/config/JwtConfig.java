package org.mdental.authcore.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.mdental.authcore.infrastructure.security.KeyGenerator;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     *
     * @param patterns the patterns from configuration
     * @return the list of allowed issuer patterns
     */
    @Bean(name = "allowedIssuerPatterns")
    public List<String> allowedIssuerPatterns(
            @Value("${mdental.auth.allowed-issuer-patterns}") String patterns
    ) {
        return List.of(patterns.split(","));
    }

    /**
     * Custom JwtTokenProvider that uses RSA keys.
     *
     * @return the token provider
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new RsaJwtTokenProvider(
                issuer,
                keyGenerator.getPrivateKey(),
                keyGenerator.getPublicKey(),
                accessTtl,
                refreshTtl,
                kidValue
        );
    }
}