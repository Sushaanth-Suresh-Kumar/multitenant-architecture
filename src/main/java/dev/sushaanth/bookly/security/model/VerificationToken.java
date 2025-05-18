package dev.sushaanth.bookly.security.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to store verification tokens used for email verification during registration.
 * Supports both library and employee registrations with a two-step OTP verification process.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {
    /**
     * Unique identifier for the verification token
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Email address being verified
     */
    @Column(nullable = false)
    private String email;

    /**
     * OTP token sent to the email
     */
    @Column(nullable = false)
    private String token;

    /**
     * Flag indicating if the token has been verified
     */
    @Column(nullable = false)
    private boolean verified = false;

    /**
     * Date and time when the token expires
     */
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    /**
     * JSON representation of the registration data
     * Temporarily stores the registration request until verification is complete
     */
    @Column(name = "registration_data", columnDefinition = "TEXT")
    private String registrationData;

    /**
     * Type of registration ("LIBRARY" or "EMPLOYEE")
     */
    @Column(name = "registration_type")
    private String registrationType;

    /**
     * Tenant ID for employee registrations
     * This field is used when an employee is being invited to join an existing tenant
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /**
     * Date and time when the token was created
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Number of verification attempts made with this token
     */
    @Column(name = "attempt_count")
    private int attemptCount = 0;

    /**
     * Default constructor
     */
    public VerificationToken() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor with essential fields
     *
     * @param email Email address
     * @param token OTP token
     * @param expiryDate Expiry date
     * @param registrationType Type of registration
     */
    public VerificationToken(String email, String token, LocalDateTime expiryDate, String registrationType) {
        this.email = email;
        this.token = token;
        this.expiryDate = expiryDate;
        this.registrationType = registrationType;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Check if the token has expired
     *
     * @return true if the token has expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Increment the attempt count
     */
    public void incrementAttemptCount() {
        this.attemptCount++;
    }

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

    public String getRegistrationData() {
        return registrationData;
    }

    public void setRegistrationData(String registrationData) {
        this.registrationData = registrationData;
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

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    @Override
    public String toString() {
        return "VerificationToken{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", verified=" + verified +
                ", expiryDate=" + expiryDate +
                ", registrationType='" + registrationType + '\'' +
                ", createdAt=" + createdAt +
                ", attemptCount=" + attemptCount +
                '}';
    }
}