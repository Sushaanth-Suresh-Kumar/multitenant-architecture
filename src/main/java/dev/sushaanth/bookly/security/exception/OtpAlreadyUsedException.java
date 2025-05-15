package dev.sushaanth.bookly.security.exception;

public class OtpAlreadyUsedException extends RuntimeException {
    public OtpAlreadyUsedException(String message) {
        super(message);
    }

    public OtpAlreadyUsedException(String message, Throwable cause) {
        super(message, cause);
    }
}