package dev.sushaanth.bookly.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for creating new employee invitations.
 * <p>
 * This record contains the data needed to invite a new employee to a tenant.
 * It is used as the request body in the invitation creation endpoint.
 */
public record InvitationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,
        String message
) {}