package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.repository.AppUserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Admins manage accounts; cashiers can log in but can't touch accounts. Uses real tokens. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserManagementTests {

	private static final String CASHIER = "{\"username\":\"sara\",\"password\":\"sara-password-1\",\"role\":\"CASHIER\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository userRepository;

	private String adminToken;

	@BeforeEach
	void logInAsAdmin() throws Exception {
		adminToken = tokenFor("admin", "admin-test-password");
	}

	@Test
	void adminCreatesACashierWhoCanLogIn() throws Exception {
		asAdmin(post("/api/users"), CASHIER)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("sara"))
				.andExpect(jsonPath("$.role").value("CASHIER"))
				.andExpect(jsonPath("$.enabled").value(true))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		String cashierToken = tokenFor("sara", "sara-password-1");
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + cashierToken))
				.andExpect(jsonPath("$.roles[0]").value("CASHIER"));
	}

	@Test
	void passwordIsStoredAsABcryptHash() throws Exception {
		asAdmin(post("/api/users"), CASHIER).andExpect(status().isCreated());

		String hash = userRepository.findByUsername("sara").orElseThrow().getPasswordHash();
		assertThat(hash).startsWith("{bcrypt}$2").doesNotContain("sara-password-1");
	}

	@Test
	void cashierCannotManageAccounts() throws Exception {
		asAdmin(post("/api/users"), CASHIER).andExpect(status().isCreated());
		String cashierToken = tokenFor("sara", "sara-password-1");

		mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + cashierToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("Access denied: your role is not allowed to do this"));
		mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + cashierToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"hacker\",\"password\":\"hacker-password\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isForbidden());
	}

	// The role written in the token at login is what the access rules check
	@Test
	void cashierTokenIsLimitedToTheCashierRole() throws Exception {
		asAdmin(post("/api/users"), CASHIER).andExpect(status().isCreated());
		String cashier = "Bearer " + tokenFor("sara", "sara-password-1");

		mockMvc.perform(get("/api/products").header("Authorization", cashier)).andExpect(status().isOk());
		mockMvc.perform(post("/api/customers").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Youssef\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/products").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Cable\",\"price\":49.90}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void usernameMustBeUnique() throws Exception {
		asAdmin(post("/api/users"), CASHIER).andExpect(status().isCreated());

		asAdmin(post("/api/users"), CASHIER.replace("sara\"", "SARA\""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Username sara is already taken"));
	}

	@Test
	void invalidAccountIsRejected() throws Exception {
		asAdmin(post("/api/users"), "{\"username\":\"a b\",\"password\":\"short\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists())
				.andExpect(jsonPath("$.errors.password").exists())
				.andExpect(jsonPath("$.errors.role").exists());
	}

	@Test
	void deletedUserCanNoLongerLogIn() throws Exception {
		long id = idOf(asAdmin(post("/api/users"), CASHIER));

		mockMvc.perform(delete("/api/users/" + id).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());

		login("sara", "sara-password-1").andExpect(status().isUnauthorized());
	}

	@Test
	void adminCannotDeleteTheirOwnAccount() throws Exception {
		long adminId = userRepository.findByUsername("admin").orElseThrow().getId();

		mockMvc.perform(delete("/api/users/" + adminId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("You cannot delete your own account"));
	}

	private ResultActions asAdmin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
			String body) throws Exception {
		return mockMvc.perform(request.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions login(String username, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
	}

	private String tokenFor(String username, String password) throws Exception {
		String json = login(username, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.accessToken");
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
