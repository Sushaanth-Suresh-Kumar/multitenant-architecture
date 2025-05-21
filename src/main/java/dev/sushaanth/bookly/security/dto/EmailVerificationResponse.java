package dev.sushaanth.bookly.security.dto;

import java.time.LocalDateTime;

public record EmailVerificationResponse(
        String email,
        String message,
        LocalDateTime expiryTime
) {}