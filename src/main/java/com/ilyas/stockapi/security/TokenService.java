package com.ilyas.stockapi.security;

import com.ilyas.stockapi.dto.LoginResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Creates the signed JWT returned by POST /api/auth/login. */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    public TokenService(JwtEncoder jwtEncoder, SecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public LoginResponse issue(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.tokenValidity());
        List<String> roles = rolesOf(authentication);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("stock-api")
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", properties.tokenValidity().toSeconds());
    }

    // "ROLE_ADMIN" -> "ADMIN" (turned back into ROLE_ADMIN when the token is read). Other
    // authorities are skipped, e.g. FACTOR_PASSWORD, which Spring Security 7 adds after a
    // password login to record how the user authenticated: it is not a role.
    public static List<String> rolesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .toList();
    }
}
