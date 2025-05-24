package org.mdental.security.jwt;

import org.junit.jupiter.api.Test;
import org.mdental.security.autoconfig.JwtProps;

import java.time.Clock;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderHsTest {

    @Test
    void createAndValidateToken_withHs256Secret_shouldSucceed() {
        JwtProps props = new JwtProps();
        props.setIssuer("mdental.test");
        props.setAccessTtl(3600);
        props.setSecret(Base64.getEncoder().encodeToString("super-secret-key-32bytes".getBytes()));

        JwtTokenProvider provider = new JwtTokenProvider(props, Clock.systemUTC());

        UUID id = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();

        String token = provider.createToken(id, "user", "u@mdental.org", tenant, Set.of());
        assertTrue(provider.validateToken(token));
        assertEquals(id, provider.parseToken(token).id());
    }
}
