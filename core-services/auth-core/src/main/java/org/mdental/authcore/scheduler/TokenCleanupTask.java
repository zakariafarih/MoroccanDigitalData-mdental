package org.mdental.authcore.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.repository.FailedLoginAttemptRepository;
import org.mdental.authcore.domain.service.AuthService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Background task for cleaning up expired tokens and login attempts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {
    private final AuthService authService;
    private final FailedLoginAttemptRepository failedLoginAttemptRepository;

    /**
     * Clean up expired tokens daily.
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM every day
    public void cleanupExpiredTokens() {
        log.info("Running scheduled cleanup of expired tokens");
        int count = authService.cleanupExpiredTokens();
        log.info("Deleted {} expired tokens", count);
    }

    /**
     * Clean up old failed login attempts daily.
     */
    @Scheduled(cron = "0 30 2 * * ?") // 2:30 AM every day
    public void cleanupOldLoginAttempts() {
        log.info("Running scheduled cleanup of old failed login attempts");
        Instant before = Instant.now().minus(30, ChronoUnit.DAYS);
        long count = failedLoginAttemptRepository.deleteByAttemptedAtBefore(before);
        log.info("Deleted {} old failed login attempts", count);
    }
}