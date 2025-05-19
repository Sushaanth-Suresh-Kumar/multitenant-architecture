package dev.sushaanth.bookly.exception;

import dev.sushaanth.bookly.security.exception.InvitationExpiredException;
import dev.sushaanth.bookly.tenant.exception.InvalidTenantException;
import dev.sushaanth.bookly.tenant.exception.TenantAlreadyExistsException;
import dev.sushaanth.bookly.tenant.exception.TenantCreationException;
import dev.sushaanth.bookly.tenant.exception.TenantNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
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

    /**
     * Handles {@link InvitationExpiredException} by creating a properly formatted
     * Problem Details response according to RFC 7807.
     * <p>
     * This handler responds with HTTP 410 (Gone) status code, which is the appropriate
     * status for resources that no longer exist but once did. In this case, it indicates
     * that an invitation was valid but has expired due to time limitations.
     * <p>
     * The response follows the Problem Details for HTTP APIs specification (RFC 7807)
     * and includes:
     * <ul>
     *   <li>A HTTP 410 Gone status code</li>
     *   <li>A detailed error message from the exception</li>
     *   <li>A descriptive title ("Invitation Expired")</li>
     *   <li>A type URI that can be used to identify this class of error</li>
     *   <li>A timestamp indicating when the error occurred</li>
     * </ul>
     *
     * @param ex The InvitationExpiredException thrown when a user attempts to use an expired invitation
     * @return A {@link ProblemDetail} object containing structured information about the error
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807: Problem Details for HTTP APIs</a>
     */
    @ExceptionHandler(InvitationExpiredException.class)
    public ProblemDetail handleInvitationExpiredException(InvitationExpiredException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.GONE,
                ex.getMessage()
        );
        problemDetail.setTitle("Invitation Expired");
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/invitation-expired"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}