package dev.sushaanth.bookly.security.scheduler;

import dev.sushaanth.bookly.security.repository.EmployeeInvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InvitationCleanUpTask {
    private static final Logger logger = LoggerFactory.getLogger(InvitationCleanUpTask.class);
    private final EmployeeInvitationRepository invitationRepository;

    public InvitationCleanUpTask(EmployeeInvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Scheduled(cron = "0 0 0 * * *") // Run once daily at midnight
    public void cleanupExpiredInvitations() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deleted = invitationRepository.deleteAllByExpiresAtBeforeAndUsedFalse(now);
            logger.info("Cleaned up {} expired employee invitations", deleted);
        } catch (Exception e) {
            logger.error("Error cleaning up expired invitations", e);
        }
    }
}
