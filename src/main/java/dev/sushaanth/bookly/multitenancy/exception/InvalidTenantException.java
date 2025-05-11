package dev.sushaanth.bookly.multitenancy.exception;

/**
 * Exception thrown when a tenant is invalid or missing.
 * This exception will be handled by GlobalExceptionHandler
 * to produce a Problem Details response.
 */
public class InvalidTenantException extends RuntimeException {

    public InvalidTenantException(String message) {
        super(message);
    }
}

