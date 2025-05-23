package org.mdental.authcore.web.controller;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.mdental.authcore.infrastructure.security.KeyGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for exposing the JWKS endpoint.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {
    private final KeyGenerator keyGenerator;

    /**
     * JWKS endpoint.
     *
     * @return JWKS document
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey publicKey = keyGenerator.getPublicKey();

        JWK jwk = new RSAKey.Builder(publicKey)
                .keyID("auth-core-key")
                .build();

        return new JWKSet(jwk).toJSONObject();
    }
}