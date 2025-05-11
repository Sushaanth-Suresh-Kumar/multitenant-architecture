package dev.sushaanth.bookly.exception;

import dev.sushaanth.bookly.multitenancy.exception.InvalidTenantException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler that converts application exceptions to standardized
 * Problem Details responses (RFC 7807).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles InvalidTenantException by converting it to a standardized Problem Detail response.
     *
     * @param ex The InvalidTenantException that was thrown
     * @return A ProblemDetail with information about the tenant error
     */
    @ExceptionHandler(InvalidTenantException.class)
    public ProblemDetail handleInvalidTenantException(InvalidTenantException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        
        problemDetail.setTitle("Tenant Error");
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/invalid-tenant"));
        
        // Extract tenant ID from exception message if available
        String message = ex.getMessage();
        if (message != null && message.contains("Invalid tenant: ")) {
            String tenant = message.substring(message.indexOf("Invalid tenant: ") + 16);
            problemDetail.setProperty("tenant", tenant);
        } else if (message != null && message.contains("Tenant header is required")) {
            problemDetail.setProperty("tenant", "missing");
        }
        
        // Add timestamp
        problemDetail.setProperty("timestamp", Instant.now());
        
        return problemDetail;
    }
}

