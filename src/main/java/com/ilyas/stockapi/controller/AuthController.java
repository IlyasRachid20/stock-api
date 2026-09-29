package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.LoginRequest;
import com.ilyas.stockapi.dto.LoginResponse;
import com.ilyas.stockapi.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    // A wrong username, a wrong password and a disabled account all return the same 401,
    // so the response doesn't reveal which usernames exist
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return tokenService.issue(authentication);
    }

    // Who the current token belongs to
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of("username", authentication.getName(), "roles", TokenService.rolesOf(authentication));
    }
}
