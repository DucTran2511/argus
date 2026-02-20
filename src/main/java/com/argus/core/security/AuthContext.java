package com.argus.core.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class to extract the current authenticated user from the Spring
 * Security context.
 * Safe to call from any controller or service — returns the user identity from
 * the JWT.
 */
public final class AuthContext {

    private AuthContext() {
    }

    /**
     * Extracts the AuthenticatedUser from the current request's JWT.
     *
     * @return AuthenticatedUser with supabaseUid, email, and role
     * @throws IllegalStateException if no authenticated user in context
     */
    public static AuthenticatedUser currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return new AuthenticatedUser(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("role"));
    }

    /**
     * Returns the Supabase UID (JWT "sub" claim) of the current user.
     */
    public static String currentUserId() {
        return currentUser().supabaseUid();
    }
}
