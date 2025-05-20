package dev.sushaanth.bookly.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BooklyException.class)
    public ProblemDetail handleBooklyException(BooklyException ex) {
        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                ex.getMessage()
        );

        problemDetail.setTitle(getErrorTitle(ex.getErrorCode()));
        problemDetail.setType(URI.create("https://api.bookly.dev/errors/" + ex.getErrorCode().name().toLowerCase()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("code", ex.getErrorCode().name());

        return problemDetail;
    }

    private HttpStatus mapErrorCodeToStatus(BooklyException.ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case INVALID_OTP, INVALID_TENANT -> HttpStatus.BAD_REQUEST;
            case EXPIRED_OTP, EXPIRED_INVITATION -> HttpStatus.GONE;
            case ALREADY_USED_OTP -> HttpStatus.CONFLICT;
            case TENANT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case TENANT_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case TENANT_CREATION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String getErrorTitle(BooklyException.ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_CREDENTIALS -> "Authentication Failed";
            case INVALID_OTP -> "Invalid Verification Code";
            case EXPIRED_OTP -> "Verification Code Expired";
            case ALREADY_USED_OTP -> "Verification Code Already Used";
            case EXPIRED_INVITATION -> "Invitation Expired";
            case TENANT_NOT_FOUND -> "Tenant Not Found";
            case TENANT_ALREADY_EXISTS -> "Tenant Already Exists";
            case TENANT_CREATION_FAILED -> "Tenant Creation Failed";
            case INVALID_TENANT -> "Invalid Tenant";
            default -> "Error";
        };
    }
}