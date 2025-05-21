package dev.sushaanth.bookly.security.dto;

import java.util.UUID;

public record RegistrationResponse(
        UUID userId,
        String username,
        String email,
        String role,
        UUID tenantId
) {}