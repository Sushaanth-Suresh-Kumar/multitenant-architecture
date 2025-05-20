package dev.sushaanth.bookly.security.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sushaanth.bookly.security.dto.RegistrationRequest;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
public class VerificationToken {
    private static final Logger logger = LoggerFactory.getLogger(VerificationToken.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "registration_type")
    private String registrationType;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Store JSON serialized registration data
    @Column(name = "registration_data", columnDefinition = "TEXT")
    private String registrationDataJson;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getRegistrationType() {
        return registrationType;
    }

    public void setRegistrationType(String registrationType) {
        this.registrationType = registrationType;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Methods to handle JSON serialization/deserialization
    public void setRegistrationData(RegistrationRequest request) {
        try {
            // Don't serialize the password for security
            RegistrationRequest sanitized = new RegistrationRequest(
                    request.username(),
                    request.firstName(),
                    request.lastName(),
                    request.email(),
                    "[PROTECTED]", // Don't store raw password
                    request.libraryName()
            );
            this.registrationDataJson = objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize registration data", e);
            throw new RuntimeException("Failed to process registration data", e);
        }
    }

    public RegistrationRequest getRegistrationData() {
        if (registrationDataJson == null || registrationDataJson.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(registrationDataJson, RegistrationRequest.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize registration data", e);
            return null;
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}