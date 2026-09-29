package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** The full login flow with real tokens (no @WithMockUser). The admin account comes from the test settings. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthTests {

	private static final String ADMIN_LOGIN = "{\"username\":\"admin\",\"password\":\"admin-test-password\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void loginReturnsABearerToken() throws Exception {
		login(ADMIN_LOGIN)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(8 * 60 * 60))
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void tokenGivesAccessToTheApi() throws Exception {
		mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + adminToken()))
				.andExpect(status().isOk());
	}

	@Test
	void meShowsWhoTheTokenBelongsTo() throws Exception {
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + adminToken()))
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.roles.length()").value(1))
				.andExpect(jsonPath("$.roles[0]").value("ADMIN"));
	}

	@Test
	void usernameIsNotCaseSensitive() throws Exception {
		login("{\"username\":\" ADMIN \",\"password\":\"admin-test-password\"}").andExpect(status().isOk());
	}

	@Test
	void requestWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error", startsWith("Authentication required")));
	}

	@Test
	void wrongPasswordAndUnknownUserGiveTheSameAnswer() throws Exception {
		login("{\"username\":\"admin\",\"password\":\"wrong-password\"}")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid username or password"));
		login("{\"username\":\"nobody\",\"password\":\"whatever-password\"}")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid username or password"));
	}

	@Test
	void emptyLoginReturns400() throws Exception {
		login("{\"username\":\"\",\"password\":\"\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void garbageTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/products").header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid or expired token: log in again with POST /api/auth/login"));
	}

	// What Swagger UI sent after the example response was pasted into "Authorize"
	@Test
	void loginStillWorksWhenABrokenTokenIsSentWithIt() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(ADMIN_LOGIN)
						.header("Authorization", "Bearer \"accessToken\": \"string\",   \"tokenType\": \"string\",   \"expiresIn\": 0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	// What happens 8 hours later, when Swagger UI still sends the old token
	@Test
	void loginStillWorksWhenAnExpiredTokenIsSentWithIt() throws Exception {
		String expired = sign(jwtEncoder, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(ADMIN_LOGIN)
						.header("Authorization", "Bearer " + expired))
				.andExpect(status().isOk());
	}

	@Test
	void swaggerDoesNotSendTheTokenToTheLoginEndpoint() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty())
				.andExpect(jsonPath("$.security[0].bearer-jwt").exists());
	}

	@Test
	void tamperedTokenReturns401() throws Exception {
		String token = adminToken();
		// Change one character in the middle of the signature (the part after the last dot).
		// Not the last character: in base64 its lowest bits can be unused padding, so changing
		// it may leave the decoded signature identical.
		int i = token.lastIndexOf('.') + 10;
		String tampered = token.substring(0, i) + (token.charAt(i) == 'A' ? 'B' : 'A') + token.substring(i + 1);

		mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + tampered))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenReturns401() throws Exception {
		String expired = sign(jwtEncoder, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));

		mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + expired))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSignedWithAnotherKeyReturns401() throws Exception {
		SecretKeySpec otherKey = new SecretKeySpec(
				"some-other-secret-that-is-32-bytes-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		String forged = sign(new NimbusJwtEncoder(new ImmutableSecret<>(otherKey)),
				Instant.now(), Instant.now().plusSeconds(3600));

		mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + forged))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void publicEndpointsNeedNoToken() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void apiDocsOfferTheAuthorizeButton() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"));
	}

	private ResultActions login(String body) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String adminToken() throws Exception {
		String json = login(ADMIN_LOGIN).andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(json, "$.accessToken");
		assertThat(token).isNotBlank();
		return token;
	}

	private static String sign(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("admin")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("roles", List.of("ADMIN"))
				.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

}
