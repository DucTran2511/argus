package com.argus.core.security;

/**
 * Lightweight record representing the authenticated user extracted from a
 * Supabase JWT.
 * Contains only identity claims — no authorization/role logic.
 */
public record AuthenticatedUser(
        String supabaseUid, // JWT "sub" claim
        String email, // JWT "email" claim
        String role // JWT "role" claim (Supabase default: "authenticated")
) {
}
