package com.ilyas.stockapi;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The API also serves the built React dashboard. src/test/resources/static/ holds a tiny stand-in
 * for the real build (the Dockerfile puts the real one in src/main/resources/static/).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardServingTests {

	@Autowired
	private MockMvc mockMvc;

	// "/" is Spring's welcome page: it forwards to index.html (a real server follows the forward, MockMvc doesn't)
	@Test
	void dashboardOpensWithoutLoggingIn() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html"));
	}

	// React handles these pages itself: opening or refreshing them must return the dashboard, not a 404
	@ParameterizedTest
	@ValueSource(strings = {"/admin", "/admin/login", "/admin/sales/new", "/admin/products", "/admin/users", "/shop", "/p/1-galaxy-s26"})
	void dashboardPagesReturnTheDashboard(String page) throws Exception {
		mockMvc.perform(get(page))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<div id=\"root\">")))
				.andExpect(header().string("Cache-Control", containsString("no-cache")));
	}

	@Test
	void builtFilesAreServedAndCachedForAYear() throws Exception {
		mockMvc.perform(get("/assets/app-123abc.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("test asset")))
				.andExpect(header().string("Cache-Control", containsString("max-age=31536000")));
	}

	@Test
	void missingFilesAreStill404() throws Exception {
		mockMvc.perform(get("/assets/missing.js")).andExpect(status().isNotFound());
		mockMvc.perform(get("/missing.png")).andExpect(status().isNotFound());
	}

	@Test
	void theApiIsNotReplacedByTheDashboard() throws Exception {
		// Without a token: the API refuses, it doesn't fall back to the dashboard
		mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void unknownApiPathsAreAJson404() throws Exception {
		mockMvc.perform(get("/api/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Not Found"));
	}

	@Test
	void swaggerAndHealthStillWork() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
		mockMvc.perform(get("/v3/api-docs")).andExpect(jsonPath("$.info.title").value("Stock API"));
	}

}
