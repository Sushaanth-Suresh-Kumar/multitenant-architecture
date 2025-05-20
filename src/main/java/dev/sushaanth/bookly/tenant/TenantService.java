package dev.sushaanth.bookly.tenant;

import dev.sushaanth.bookly.exception.BooklyException;
import dev.sushaanth.bookly.exception.BooklyException.ErrorCode;
import dev.sushaanth.bookly.tenant.dto.TenantCreateRequest;
import dev.sushaanth.bookly.tenant.dto.TenantResponse;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TenantService {
    @Value("${multitenancy.schema.prefix}")
    private String schemaPrefix;

    private final TenantRepository tenantRepository;
    private final DataSource dataSource;

    public TenantService(TenantRepository tenantRepository, DataSource dataSource) {
        this.tenantRepository = tenantRepository;
        this.dataSource = dataSource;
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::mapToTenantResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TenantResponse createTenant(TenantCreateRequest request) {
        // Check if tenant display name exists
        if (tenantRepository.findByDisplayName(request.displayName()).isPresent()) {
            throw new BooklyException(
                    ErrorCode.TENANT_ALREADY_EXISTS,
                    "Tenant with name " + request.displayName() + " already exists");
        }

        try {
            UUID id = UUID.randomUUID();
            String schemaName = generateSchemaName(id);

            // Create tenant entity
            Tenant tenant = new Tenant(request.displayName(), request.description());
            tenant.setSchemaName(schemaName);
            tenant = tenantRepository.save(tenant);

            // Create schema using Flyway
            createTenantSchema(schemaName);

            return mapToTenantResponse(tenant);
        } catch (Exception e) {
            throw new BooklyException(
                    ErrorCode.TENANT_CREATION_FAILED,
                    "Failed to create tenant: " + e.getMessage());
        }
    }

    private String generateSchemaName(UUID id) {
        return schemaPrefix + id.toString().replace("-", "");
    }

    private void createTenantSchema(String schemaName) {
        // Create schema with Flyway migrations
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenants")
                .load();

        flyway.migrate();
    }

    private TenantResponse mapToTenantResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getDisplayName(),
                tenant.getDescription(),
                tenant.getSchemaName()
        );
    }
}