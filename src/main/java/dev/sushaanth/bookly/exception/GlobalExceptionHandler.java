package dev.sushaanth.bookly.exception;

import dev.sushaanth.bookly.tenant.exception.InvalidTenantException;
import dev.sushaanth.bookly.tenant.exception.TenantAlreadyExistsException;
import dev.sushaanth.bookly.tenant.exception.TenantCreationException;
import dev.sushaanth.bookly.tenant.exception.TenantNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler that converts application exceptions to standardized
 * Problem Details responses (RFC 7807).
 *
 * This handler provides a central location for handling all application-specific
 * exceptions, ensuring consistent error response formatting across the application.
 * It follows the RFC 7807 standard for HTTP API problem details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles InvalidTenantException which occurs when a tenant identifier is missing,
     * invalid, or references a non-existent tenant.
     *
     * @param ex The exception containing tenant validation error details
     * @return A formatted problem detail with BAD_REQUEST status
     */
    @ExceptionHandler(InvalidTenantException.class)
    public ProblemDetail handleInvalidTenantException(InvalidTenantException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        problemDetail.setTitle("Tenant Error");
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/invalid-tenant"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Handles TenantAlreadyExistsException which occurs when attempting to create a tenant
     * with a display name that already exists in the system.
     *
     * @param ex The exception with details about the duplicate tenant
     * @return A formatted problem detail with CONFLICT status
     */
    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ProblemDetail handleTenantAlreadyExistsException(TenantAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        problemDetail.setTitle("Tenant Already Exists");
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/tenant-already-exists"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Handles TenantCreationException which occurs when the system encounters an error
     * during tenant schema creation or initialization.
     *
     * @param ex The exception with details about the tenant creation failure
     * @return A formatted problem detail with INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(TenantCreationException.class)
    public ProblemDetail handleTenantCreationException(TenantCreationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );

        problemDetail.setTitle("Tenant Creation Failed");
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/tenant-creation-failed"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Handles TenantNotFoundException which occurs when attempting to access a tenant
     * that does not exist in the system.
     *
     * @param ex The exception with details about the tenant lookup failure
     * @return A formatted problem detail with NOT_FOUND status
     */
    @ExceptionHandler(TenantNotFoundException.class)
    public ProblemDetail handleTenantNotFoundException(TenantNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        problemDetail.setTitle("Tenant Not Found");
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/tenant-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}