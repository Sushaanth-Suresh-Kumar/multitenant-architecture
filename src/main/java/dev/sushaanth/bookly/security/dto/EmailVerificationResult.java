package dev.sushaanth.bookly.security.dto;

public record EmailVerificationResult(
        boolean verified,
        String email,
        String message
) {}
