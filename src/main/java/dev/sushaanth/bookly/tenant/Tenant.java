package dev.sushaanth.bookly.tenant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "public")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String schemaName;

    @NotEmpty(message = "Display name cannot be empty")
    private String displayName;

    private String description;

    public Tenant() {
    }

    public Tenant(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

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
}