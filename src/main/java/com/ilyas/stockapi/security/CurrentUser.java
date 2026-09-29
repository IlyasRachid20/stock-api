package com.ilyas.stockapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The username of whoever sent the current request, for "created by" fields. */
public final class CurrentUser {

    private CurrentUser() {
    }

    // "system" when there is no logged-in user, e.g. work done at startup
    public static String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication == null || !authentication.isAuthenticated()) ? "system" : authentication.getName();
    }
}
