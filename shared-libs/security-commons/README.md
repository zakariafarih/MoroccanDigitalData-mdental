# MDental JWT Security Commons

This module provides JWT-based authentication for MDental microservices.

## Features

- JWT token generation, verification, and parsing
- Support for both HS256 (shared secret) and RS256 (public/private key) signing
- Spring Security integration for both MVC and WebFlux applications
- Automatic header propagation for downstream services
- Compliant with RFC-6750 for error responses

## Configuration

Add the following properties to your `application.yml` or `application.properties`:

```yaml
mdental:
  auth:
    jwt:
      # Choose ONE of the following authentication methods:
      
      # Option 1: HMAC (symmetric) with shared secret
      secret: "base64-encoded-secret-key-at-least-256-bits"
      
      # Option 2: RSA (asymmetric) with key pair
      public-key: "base64-encoded-public-key"
      private-key: "base64-encoded-private-key"
      
      # Common settings
      issuer: "mdental.org"
      access-ttl: 3600  # access token TTL in seconds (1 hour)
      refresh-ttl: 2592000  # refresh token TTL in seconds (30 days)