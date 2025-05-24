package org.mdental.authcore.service;

import lombok.extern.slf4j.Slf4j;
import org.mdental.security.jwt.JwtIssuerValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class IssuerValidator implements JwtIssuerValidator {

    private final List<Pattern> allowedPatterns;

    public IssuerValidator(@Qualifier("allowedIssuerPatterns")
            @Value("#{'${mdental.auth.allowed-issuer-patterns}'.split(',')}")
            List<String> patterns
    ) {
        this.allowedPatterns = patterns.stream()
                .map(p -> Pattern.compile(p.trim().replace("*", ".*")))
                .toList();
        log.info("IssuerValidator initialized with {}", patterns);
    }

    @Override
    public boolean validate(String issuer) {
        if (issuer == null || issuer.isBlank()) {
            log.warn("Rejecting empty issuer");
            return false;
        }
        return allowedPatterns.stream()
                .anyMatch(p -> p.matcher(issuer).matches());
    }
}
