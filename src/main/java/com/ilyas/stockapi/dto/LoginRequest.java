package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

/** Body of POST /api/auth/login. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {

    public LoginRequest {
        username = (username == null) ? null : username.trim().toLowerCase(Locale.ROOT);
    }
}
