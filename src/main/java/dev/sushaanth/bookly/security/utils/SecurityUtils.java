package dev.sushaanth.bookly.security.utils;

import dev.sushaanth.bookly.exception.BooklyException;
import dev.sushaanth.bookly.exception.BooklyException.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {
        // Utility class, no instantiation
    }

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getDetails() instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) auth.getDetails();
            if (details.containsKey("userId")) {
                Object userId = details.get("userId");
                if (userId instanceof UUID) {
                    return (UUID) userId;
                } else if (userId instanceof String) {
                    return UUID.fromString((String) userId);
                }
            }
        }
        throw new BooklyException(ErrorCode.INVALID_CREDENTIALS, "User ID not available in security context");
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new BooklyException(ErrorCode.INVALID_CREDENTIALS, "Not authenticated");
    }
}
