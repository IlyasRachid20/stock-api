package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST and PUT /api/customers. */
public record CustomerRequest(
        @NotBlank @Size(max = 100) String name,
        @Email @Size(max = 150) String email,
        @Size(max = 30) String phone) {
}
