package com.ilyas.stockapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** Body of POST and PUT /api/customers. */
public record CustomerRequest(
        @NotBlank @Size(max = 100) String name,
        @Email @Size(max = 150) String email,
        @Size(max = 30) String phone) {

    // Runs when the JSON is read, before validation: " Ahmed@Test.com " and "ahmed@test.com"
    // are the same email, and a blank email means no email
    public CustomerRequest {
        name = (name == null) ? null : name.trim();
        email = (email == null || email.isBlank()) ? null : email.trim().toLowerCase(Locale.ROOT);
        phone = (phone == null || phone.isBlank()) ? null : phone.trim();
    }
}
