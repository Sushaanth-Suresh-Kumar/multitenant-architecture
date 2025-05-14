package dev.sushaanth.bookly.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySchemaName(String schemaName);
    boolean existsBySchemaName(String schemaName);
}