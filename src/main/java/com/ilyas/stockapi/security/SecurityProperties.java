package com.ilyas.stockapi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings under "app.security" in application.properties.
 *
 * @param jwtSecret      key used to sign tokens (APP_JWT_SECRET), at least 32 bytes; random if empty
 * @param tokenValidity  how long a token stays valid after login
 * @param adminUsername  first account, created when there are no users yet
 * @param adminPassword  its password (APP_ADMIN_PASSWORD); generated and logged once if empty
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(
        String jwtSecret,
        @DefaultValue("8h") Duration tokenValidity,
        @DefaultValue("admin") String adminUsername,
        String adminPassword) {
}
