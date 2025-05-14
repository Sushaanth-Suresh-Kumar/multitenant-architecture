package dev.sushaanth.bookly.security.model;

public enum Role {
    ROLE_LIBRARY_ADMIN,
    ROLE_EMPLOYEE;

    // Helper method to get Spring Security format
    public String getAuthority() {
        return this.name();
    }
}
