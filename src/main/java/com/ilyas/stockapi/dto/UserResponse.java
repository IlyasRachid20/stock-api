package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.AppUser;
import com.ilyas.stockapi.entity.Role;

/** A user as returned by the API: never includes the password hash. */
public record UserResponse(Long id, String username, Role role, boolean enabled) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isEnabled());
    }
}
