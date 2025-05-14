package dev.sushaanth.bookly.tenant.dto;

import jakarta.validation.constraints.NotEmpty;

public record TenantCreateRequest(
        @NotEmpty(message = "Display name cannot be empty")
        String displayName,

        String description
) {}