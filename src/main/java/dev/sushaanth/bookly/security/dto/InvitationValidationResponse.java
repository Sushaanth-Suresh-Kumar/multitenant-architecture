package dev.sushaanth.bookly.security.dto;

import java.time.LocalDateTime;

public record InvitationValidationResponse(
        boolean valid,
        String email,
        String libraryName,
        String invitedByName,
        LocalDateTime expiresAt,
        String message
) {}