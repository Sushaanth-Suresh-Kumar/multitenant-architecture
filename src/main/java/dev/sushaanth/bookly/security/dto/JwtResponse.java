package dev.sushaanth.bookly.security.dto;

public record JwtResponse(
        String token,
        String username,
        String role
) {}