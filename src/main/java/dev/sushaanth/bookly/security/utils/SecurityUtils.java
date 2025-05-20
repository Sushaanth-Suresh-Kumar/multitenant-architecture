package dev.sushaanth.bookly.security.utils;

import dev.sushaanth.bookly.exception.BooklyException;
import dev.sushaanth.bookly.exception.BooklyException.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {
        // Utility class, no instantiation
    }

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            // If using JWT with tenantId claim, extract from there
            // This is a simplified example - adjust based on your actual implementation
            String userId = auth.getName();
            return UUID.fromString(userId);
        }
        throw new BooklyException(ErrorCode.INVALID_CREDENTIALS, "Not authenticated");
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new BooklyException(ErrorCode.INVALID_CREDENTIALS, "Not authenticated");
    }
}
