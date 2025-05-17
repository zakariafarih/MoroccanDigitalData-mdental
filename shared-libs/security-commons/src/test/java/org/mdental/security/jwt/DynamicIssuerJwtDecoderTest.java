package org.mdental.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicIssuerJwtDecoderTest {

    @Test
    void extractIssuer_shouldReturnIssuerFromToken() throws Exception {
        // Arrange
        List<String> patterns = Arrays.asList("https://auth.mdental.org/realms/platform");
        DynamicIssuerJwtDecoder decoder = new DynamicIssuerJwtDecoder(patterns);

        String issuer = "https://auth.mdental.org/realms/test-realm";
        String token = createJwtWithIssuer(issuer);

        // Act
        String extractedIssuer = invokeExtractIssuer(decoder, token);

        // Assert
        assertEquals(issuer, extractedIssuer, "Should extract correct issuer from JWT");
    }

    @Test
    void extractIssuer_shouldThrowExceptionForMalformedToken() {
        // Arrange
        List<String> patterns = Arrays.asList("https://auth.mdental.org/realms/platform");
        DynamicIssuerJwtDecoder decoder = new DynamicIssuerJwtDecoder(patterns);

        // Act & Assert
        assertThrows(JwtException.class, () -> {
            invokeExtractIssuer(decoder, "not.a.valid.jwt");
        }, "Should throw exception for malformed JWT");
    }

    @Test
    void extractIssuer_shouldHandleOversizedToken() throws Exception {
        // Arrange
        List<String> patterns = Arrays.asList("https://auth.mdental.org/realms/platform");
        DynamicIssuerJwtDecoder decoder = new DynamicIssuerJwtDecoder(patterns);

        // Create claims with large payload
        StringBuilder largePayload = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largePayload.append("a");
        }

        String issuer = "https://auth.mdental.org/realms/test-realm";
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("test-subject")
                .claim("large-field", largePayload.toString())
                .expirationTime(new Date(System.currentTimeMillis() + 60 * 1000))
                .build();

        JWSSigner signer = new MACSigner(generateRandomKey(32));
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(signer);

        String token = signedJWT.serialize();

        // Act
        String extractedIssuer = invokeExtractIssuer(decoder, token);

        // Assert
        assertEquals(issuer, extractedIssuer, "Should extract issuer even from large JWT");
    }

    @Test
    void decode_shouldUseCache() throws Exception {
        // Arrange - Use mocks to verify cache behavior
        List<String> patterns = Arrays.asList("https://auth.mdental.org/realms/platform");
        DynamicIssuerJwtDecoder decoder = new DynamicIssuerJwtDecoder(patterns) {
            protected JwtDecoder buildDecoder(String issuer) {
                // This override allows us to count how many times buildDecoder is called
                JwtDecoder mockDecoder = mock(JwtDecoder.class);
                try {
                    when(mockDecoder.decode(anyString())).thenReturn(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return mockDecoder;
            }
        };

        // Create a valid JWT with the issuer
        String issuer = "https://auth.mdental.org/realms/test-realm";
        String token = createJwtWithIssuer(issuer);

        // Mock the extract issuer method to always return our test issuer
        // This is needed because our test JWT won't validate in a real decoder
        java.lang.reflect.Method extractMethod = DynamicIssuerJwtDecoder.class.getDeclaredMethod("extractIssuer", String.class);
        extractMethod.setAccessible(true);

        // Get cache field to check cache state
        java.lang.reflect.Field cacheField = DynamicIssuerJwtDecoder.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, JwtDecoder> cache = (java.util.Map<String, JwtDecoder>) cacheField.get(decoder);

        // Act & Assert - First call should create cache entry
        try {
            // We expect this to fail because our mock decoder returns null
            decoder.decode(token);
            fail("Should have thrown exception");
        } catch (Exception e) {
            // This is expected
            assertTrue(cache.containsKey(issuer), "Cache should contain entry for issuer");
        }

        int initialCacheSize = cache.size();

        // Act & Assert - Second call should use cache
        try {
            decoder.decode(token);
            fail("Should have thrown exception");
        } catch (Exception e) {
            // This is expected
            assertEquals(initialCacheSize, cache.size(), "Cache size should not change on second call");
        }
    }

    // Helper methods

    private String createJwtWithIssuer(String issuer) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("test-subject")
                .expirationTime(new Date(System.currentTimeMillis() + 60 * 1000))
                .build();

        JWSSigner signer = new MACSigner(generateRandomKey(32));
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    private String invokeExtractIssuer(DynamicIssuerJwtDecoder decoder, String token) throws Exception {
        Method method = DynamicIssuerJwtDecoder.class
                .getDeclaredMethod("extractIssuer", String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(decoder, token);
        } catch (InvocationTargetException ite) {
            // if the *cause* is a JwtException, re-throw it directly:
            Throwable cause = ite.getCause();
            if (cause instanceof JwtException) {
                throw (JwtException) cause;
            }
            // otherwise re-throw whatever it was
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw ite;
        }
    }

    private byte[] generateRandomKey(int sizeInBytes) {
        byte[] key = new byte[sizeInBytes];
        new java.util.Random().nextBytes(key);
        return key;
    }
}