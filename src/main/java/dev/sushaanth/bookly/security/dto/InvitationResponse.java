package dev.sushaanth.bookly.security.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String email,
        UUID invitedById,
        String invitedByName,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean used,
        boolean expired,
        InvitationStatus status
) {
    public enum InvitationStatus {
        PENDING,
        EXPIRED,
        USED
    }

    // Constructor without status and expired fields
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

    private static InvitationStatus determineStatus(boolean used, boolean expired) {
        if (used) return InvitationStatus.USED;
        if (expired) return InvitationStatus.EXPIRED;
        return InvitationStatus.PENDING;
    }
}