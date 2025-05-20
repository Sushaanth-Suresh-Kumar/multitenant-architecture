package dev.sushaanth.bookly.exception;

public class BooklyException extends RuntimeException {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        // Auth errors
        INVALID_CREDENTIALS,
        INVALID_OTP,
        EXPIRED_OTP,
        ALREADY_USED_OTP,
        EXPIRED_INVITATION,

        // Tenant errors
        TENANT_NOT_FOUND,
        TENANT_CREATION_FAILED,
        TENANT_ALREADY_EXISTS,
        INVALID_TENANT
    }

    public BooklyException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}