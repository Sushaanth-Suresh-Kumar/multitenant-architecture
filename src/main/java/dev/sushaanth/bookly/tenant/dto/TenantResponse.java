package dev.sushaanth.bookly.tenant.dto;

import java.util.UUID;

public record TenantResponse(
        UUID id,
        String displayName,
        String description,
        String schemaName
) {
}