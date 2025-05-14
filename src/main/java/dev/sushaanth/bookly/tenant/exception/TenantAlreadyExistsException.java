package dev.sushaanth.bookly.tenant.exception;

/**
 * Exception thrown when attempting to create a tenant with a name or identifier
 * that already exists in the system.
 */
public class TenantAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new TenantAlreadyExistsException with the specified detail message.
     *
     * @param message the detail message
     */
    public TenantAlreadyExistsException(String message) {
        super(message);
    }
}