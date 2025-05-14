package dev.sushaanth.bookly.tenant.exception;

/**
 * Exception thrown when attempting to retrieve, update, or delete a tenant
 * that does not exist in the system.
 */
public class TenantNotFoundException extends RuntimeException {

    /**
     * Constructs a new TenantNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public TenantNotFoundException(String message) {
        super(message);
    }
}