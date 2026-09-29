package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** Body of POST /api/users (ADMIN only). */
public record UserRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[a-z0-9._-]+", message = "may only contain letters, digits, '.', '_' and '-'")
        String username,
        // BCrypt only uses the first 72 bytes of a password
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role) {

    public UserRequest {
        username = (username == null) ? null : username.trim().toLowerCase(Locale.ROOT);
    }
}
