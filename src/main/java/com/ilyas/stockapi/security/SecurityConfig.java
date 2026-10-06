package com.ilyas.stockapi.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // Open to everyone, with or without a token. Product pictures too: an <img> can't send a token,
    // and they are meant for the online shop anyway
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login", "/api/images/*", "/actuator/health", "/error",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                // Clients send a token on every request instead of a session cookie, so there is
                // no session to protect against CSRF and nothing to store on the server
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: logging in, the health check and the API documentation
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // Only admins manage user accounts
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // What each role may do (the table is in RoleAccessTests). Rules are checked
                        // from top to bottom and the first one that matches decides.
                        // Revenue reports are for admins: this must come before the GET rule below,
                        // which would otherwise let cashiers read them
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        // Both roles can read everything else
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "CASHIER")
                        // A cashier registers customers and records sales at the till
                        .requestMatchers(HttpMethod.POST, "/api/customers", "/api/sales", "/api/sale-items")
                                .hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/*").hasAnyRole("ADMIN", "CASHIER")
                        // Everything else under /api is for admins: products, prices, stock and every
                        // delete. Deny by default: a new endpoint stays admin-only until a rule above opens it.
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        // Outside /api there is no data: the dashboard's pages and files (it shows its own
                        // login screen), plus the public paths above. Every API call still needs a token.
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(ignoringPublicPaths())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(unauthorized())
                        .accessDeniedHandler(forbidden()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(unauthorized())
                        .accessDeniedHandler(forbidden()));
        return http.build();
    }

    // Stored hashes look like "{bcrypt}$2a$10$...", so the algorithm can be upgraded later
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecretKey jwtSigningKey(SecurityProperties properties) {
        String secret = properties.jwtSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("APP_JWT_SECRET is not set: using a random key, so tokens stop working when the app restarts");
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            return new SecretKeySpec(random, "HmacSHA256");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least 32 bytes long for HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // The token's "roles" claim (e.g. ["ADMIN"]) becomes the authority ROLE_ADMIN
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    // Public paths don't read the Authorization header at all. Otherwise an expired or broken
    // token (e.g. one Swagger UI still sends after 8 hours) would get a 401 on the login
    // request itself, the one request meant to get a new token.
    private static BearerTokenResolver ignoringPublicPaths() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        List<RequestMatcher> matchers = Arrays.stream(PUBLIC_PATHS)
                .map(path -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(path))
                .toList();
        RequestMatcher publicPaths = new OrRequestMatcher(matchers);
        return request -> publicPaths.matches(request) ? null : defaultResolver.resolve(request);
    }

    // Same {"error": "..."} shape as every other API error. A token that was sent but is invalid
    // or expired gets its own message, so the client knows to log in again.
    private static AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) -> writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                request.getHeader("Authorization") != null
                        ? "Invalid or expired token: log in again with POST /api/auth/login"
                        : "Authentication required: log in and send the token as 'Authorization: Bearer <token>'");
    }

    private static AccessDeniedHandler forbidden() {
        return (request, response, ex) -> writeError(response, HttpServletResponse.SC_FORBIDDEN,
                "Access denied: your role is not allowed to do this");
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Fixed messages without quotes or backslashes, so no JSON escaping is needed
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
