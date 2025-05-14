package dev.sushaanth.bookly.tenant;

import dev.sushaanth.bookly.tenant.dto.TenantCreateRequest;
import dev.sushaanth.bookly.tenant.dto.TenantResponse;
import dev.sushaanth.bookly.tenant.exception.TenantCreationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TenantService {
    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

    @Value("${multitenancy.schema.prefix}")
    private String schemaPrefix;

    private final TenantRepository tenantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::mapToTenantResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TenantResponse createTenant(TenantCreateRequest request) {
        try {
            // Create tenant entity
            Tenant tenant = new Tenant(
                    request.displayName(),
                    request.description()
            );

            // Generate UUID if not already set
            UUID id = UUID.randomUUID();

            // Generate schema name using UUID
            String schemaName = generateSchemaName(id);
            tenant.setSchemaName(schemaName);

            // Save tenant to database with schema name already set
            tenant = tenantRepository.save(tenant);

            // Create schema and tables for the new tenant
            createTenantSchema(schemaName);
            logger.debug("Created new tenant schema successfully");

            return mapToTenantResponse(tenant);
        } catch (Exception e) {
            logger.error("Failed to create tenant", e);
            throw new TenantCreationException("Failed to create tenant: " + e.getMessage(), e);
        }
    }

    private String generateSchemaName(UUID id) {
        // Create schema name using prefix and UUID (without dashes)
        return schemaPrefix + id.toString().replace("-", "");
    }

    private void createTenantSchema(String schemaName) {
        try {
            // Get Hibernate session
            Session session = entityManager.unwrap(Session.class);

            // 1. Create schema first - must be done before anything else
            String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
            executeStatement(session, createSchemaSQL);

            // 2. Create users table
            String createUsersTableSQL = "CREATE TABLE IF NOT EXISTS " + schemaName + ".users (" +
                    "id UUID PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "firstname VARCHAR(255) NOT NULL, " +
                    "lastname VARCHAR(255) NOT NULL, " +
                    "email VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            executeStatement(session, createUsersTableSQL);

            // 3. Create books table
            String createBooksTableSQL = "CREATE TABLE IF NOT EXISTS " + schemaName + ".books (" +
                    "id UUID PRIMARY KEY, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "author VARCHAR(255) NOT NULL, " +
                    "isbn VARCHAR(20), " +
                    "publisher VARCHAR(255), " +
                    "publication_year INT, " +
                    "description TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            executeStatement(session, createBooksTableSQL);

            // 4. Create borrowings table with foreign keys
            String createBorrowingsTableSQL = "CREATE TABLE IF NOT EXISTS " + schemaName + ".borrowings (" +
                    "id UUID PRIMARY KEY, " +
                    "book_id UUID NOT NULL, " +
                    "user_id UUID NOT NULL, " +
                    "borrow_date TIMESTAMP NOT NULL, " +
                    "due_date TIMESTAMP NOT NULL, " +
                    "return_date TIMESTAMP, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_book FOREIGN KEY (book_id) REFERENCES " + schemaName + ".books(id), " +
                    "CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES " + schemaName + ".users(id))";
            executeStatement(session, createBorrowingsTableSQL);

            // 5. Create indexes
            String createUserIndexSQL = "CREATE INDEX IF NOT EXISTS idx_users_username ON " + schemaName + ".users(username)";
            executeStatement(session, createUserIndexSQL);

            String createBookTitleIndexSQL = "CREATE INDEX IF NOT EXISTS idx_books_title ON " + schemaName + ".books(title)";
            executeStatement(session, createBookTitleIndexSQL);

            String createBookAuthorIndexSQL = "CREATE INDEX IF NOT EXISTS idx_books_author ON " + schemaName + ".books(author)";
            executeStatement(session, createBookAuthorIndexSQL);

        } catch (Exception e) {
            logger.error("Error creating tenant schema", e);
            throw new TenantCreationException("Failed to create tenant schema", e);
        }
    }

    private void executeStatement(Session session, String sql) {
        try {
            session.createNativeQuery(sql, Void.class).executeUpdate();
            logger.debug("Executed SQL successfully");
        } catch (Exception e) {
            logger.error("Failed to execute SQL: {}", sql, e);
            throw new TenantCreationException("Failed to create tenant schema", e);
        }
    }

    private String readResourceFile(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    private TenantResponse mapToTenantResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getDisplayName(),
                tenant.getDescription()
        );
    }
}