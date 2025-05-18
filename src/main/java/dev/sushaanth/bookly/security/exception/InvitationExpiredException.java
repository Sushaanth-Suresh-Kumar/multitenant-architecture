package dev.sushaanth.bookly.security.exception;

import dev.sushaanth.bookly.exception.GlobalExceptionHandler;

/**
 * Exception thrown when a user attempts to use an employee invitation
 * that has expired based on its expiration date.
 * <p>
 * This exception is thrown during the employee registration process when
 * validating invitation tokens. It will be handled by {@link GlobalExceptionHandler}
 * to produce a Problem Details response with HTTP 410 (Gone) status.
 */
public class InvitationExpiredException extends RuntimeException {

    /**
     * Constructs a new invitation expired exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method)
     */
    public InvitationExpiredException(String message) {
        super(message);
    }

    /**
     * Constructs a new invitation expired exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method)
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method)
     */
    public InvitationExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}