package org.mdental.authcore.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.mdental.security.autoconfig.JwtProps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwkConfig {

    @Bean
    public RSAKey rsaJwk(JwtProps props) throws Exception {
        // Get the raw PEM string
        String pem = props.getPublicKey();

        // Strip the header, footer, and all whitespace
        String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        // Now decode the base64
        byte[] pubBytes = Base64.getDecoder().decode(cleaned);

        // Build the RSA public key
        RSAPublicKey pub = (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(pubBytes));

        return new RSAKey.Builder(pub)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(props.getKeyId())
                .build();
    }

    @Bean
    public JWKSet jwkSet(RSAKey rsaJwk) {
        return new JWKSet(rsaJwk);
    }
}
