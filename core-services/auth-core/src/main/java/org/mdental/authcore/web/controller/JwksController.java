package org.mdental.authcore.web.controller;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.mdental.authcore.infrastructure.security.KeyRotationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {
    private final KeyRotationService keyRotationService;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return keyRotationService.getJwkSet().toJSONObject();
    }
}