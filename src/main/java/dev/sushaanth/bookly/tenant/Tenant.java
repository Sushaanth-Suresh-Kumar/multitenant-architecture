package dev.sushaanth.bookly.tenant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a tenant in the multi-tenant library management system.
 * Each tenant is a separate library with its own isolated schema.
 */
@Entity
@Table(name = "tenants", schema = "public")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String schemaName;

    @NotEmpty(message = "Display name cannot be empty")
    @Column(unique = true)
    private String displayName;

    private String description;

    /**
     * Timestamp of when the tenant was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last update to the tenant
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Flag indicating if the tenant is active
     */
    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * UUID of the user who owns/manages this tenant
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    /**
     * Default no-args constructor required by JPA
     */
    public Tenant() {
    }

    /**
     * Convenience constructor for creating a new tenant
     *
     * @param displayName Display name of the tenant (library name)
     * @param description Description of the tenant
     */
    public Tenant(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Update the last modified timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", schemaName='" + schemaName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", active=" + active +
                '}';
    }
}