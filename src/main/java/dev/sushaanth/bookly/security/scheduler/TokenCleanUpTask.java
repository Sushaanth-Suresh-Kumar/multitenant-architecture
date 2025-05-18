package dev.sushaanth.bookly.security.scheduler;

import dev.sushaanth.bookly.security.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class TokenCleanUpTask {

    private final VerificationTokenRepository tokenRepository;
    private static final Logger logger = LoggerFactory.getLogger(TokenCleanUpTask.class);

    public TokenCleanUpTask(VerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Scheduled(cron = "0 0 * * * *") // Run once per hour
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deleted = tokenRepository.deleteAllByExpiryDateBefore(now);
            logger.info("Cleaned up {} expired verification tokens", deleted);
        } catch (Exception e) {
            logger.error("Error cleaning up expired tokens", e);
        }
    }
}