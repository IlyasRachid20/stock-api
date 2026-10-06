package com.ilyas.stockapi.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The username of whoever sent the current request, for "created by" fields. */
public final class CurrentUser {

    private CurrentUser() {
    }

    // "system" when there is no logged-in user (work done at startup or on a schedule), and
    // "online shop" for a visitor of the shop (e.g. the stock taken by an online order)
    public static final String ONLINE_SHOP = "online shop";

    public static String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            return ONLINE_SHOP;
        }
        return (authentication == null || !authentication.isAuthenticated()) ? "system" : authentication.getName();
    }
}
