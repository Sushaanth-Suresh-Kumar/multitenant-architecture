package dev.sushaanth.bookly.tenant.exception;

/**
 * Exception thrown when the system encounters errors during tenant schema creation
 * or initialization process.
 */
public class TenantCreationException extends RuntimeException {

    /**
     * Constructs a new TenantCreationException with the specified detail message.
     *
     * @param message the detail message
     */
    public TenantCreationException(String message) {
        super(message);
    }

    /**
     * Constructs a new TenantCreationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public TenantCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}