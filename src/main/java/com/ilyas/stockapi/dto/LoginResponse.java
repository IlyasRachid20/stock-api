package com.ilyas.stockapi.dto;

/** Send the token back on every request as the header "Authorization: Bearer <accessToken>". */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
