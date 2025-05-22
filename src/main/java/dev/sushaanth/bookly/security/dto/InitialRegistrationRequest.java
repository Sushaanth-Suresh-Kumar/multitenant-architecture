package dev.sushaanth.bookly.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record InitialRegistrationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        String email
) {}