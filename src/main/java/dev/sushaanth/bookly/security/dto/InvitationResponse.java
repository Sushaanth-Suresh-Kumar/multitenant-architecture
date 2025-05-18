package dev.sushaanth.bookly.security.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) for employee invitation responses.
 * <p>
 * This record represents an employee invitation in API responses.
 * It includes all the details needed to display and manage the invitation.
 */
public record InvitationResponse(
        UUID id,
        String email,
        UUID invitedById,
        String invitedByName,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean used,
        boolean expired,
        String status
) {
    /**
     * Convenience constructor to create a response from an invitation entity.
     *
     * @param id Invitation UUID
     * @param email Invited employee email
     * @param invitedById Admin ID
     * @param invitedByName Admin name
     * @param createdAt Creation timestamp
     * @param expiresAt Expiration timestamp
     * @param used Whether the invitation has been used
     */
    public InvitationResponse(
            UUID id,
            String email,
            UUID invitedById,
            String invitedByName,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            boolean used) {
        this(
                id,
                email,
                invitedById,
                invitedByName,
                createdAt,
                expiresAt,
                used,
                LocalDateTime.now().isAfter(expiresAt),
                determineStatus(used, LocalDateTime.now().isAfter(expiresAt))
        );
    }

    /**
     * Helper method to determine invitation status.
     */
    private static String determineStatus(boolean used, boolean expired) {
        if (used) return "USED";
        if (expired) return "EXPIRED";
        return "PENDING";
    }
}