package org.mdental.authcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
@Slf4j
public class IssuerValidator implements Predicate<String> {

    private final List<Pattern> allowedIssuerPatterns;

    public IssuerValidator(@Value("#{'${mdental.auth.allowed-issuer-patterns}'.split(',')}") List<String> patterns) {
        this.allowedIssuerPatterns = patterns.stream()
                .map(pattern -> Pattern.compile(pattern.trim().replace("*", ".*")))
                .toList();

        log.info("Initialized issuer validator with patterns: {}", patterns);
    }

    @Override
    public boolean test(String issuer) {
        if (issuer == null || issuer.isEmpty()) {
            log.warn("Rejecting null or empty issuer");
            return false;
        }

        for (Pattern pattern : allowedIssuerPatterns) {
            if (pattern.matcher(issuer).matches()) {
                log.debug("Issuer '{}' matches allowed pattern '{}'", issuer, pattern);
                return true;
            }
        }

        log.warn("Issuer '{}' does not match any allowed pattern", issuer);
        return false;
    }
}