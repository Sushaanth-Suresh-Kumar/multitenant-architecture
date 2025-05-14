package dev.sushaanth.bookly.tenant.exception;

/**
 * Exception thrown when a tenant is invalid, missing, or cannot be resolved.
 * This exception will be handled by GlobalExceptionHandler to produce a
 * Problem Details response with HTTP 400 (Bad Request) status.
 */
public class InvalidTenantException extends RuntimeException {

    /**
     * Constructs a new InvalidTenantException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidTenantException(String message) {
        super(message);
    }
}