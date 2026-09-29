package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DuplicateEmailTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void creatingCustomerWithUsedEmailReturns409() throws Exception {
		postCustomer("{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\"}").andExpect(status().isCreated());

		postCustomer("{\"name\":\"Other Ahmed\",\"email\":\"ahmed@test.com\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Email ahmed@test.com is already used by another customer"));

		mockMvc.perform(get("/api/customers")).andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void customersWithoutEmailAreAllowed() throws Exception {
		postCustomer("{\"name\":\"Ahmed\"}").andExpect(status().isCreated());
		postCustomer("{\"name\":\"Sara\"}").andExpect(status().isCreated());
	}

	@Test
	void updatingCustomerToAnotherCustomersEmailReturns409() throws Exception {
		postCustomer("{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\"}").andExpect(status().isCreated());
		long saraId = idOf(postCustomer("{\"name\":\"Sara\",\"email\":\"sara@test.com\"}"));

		mockMvc.perform(put("/api/customers/" + saraId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Sara\",\"email\":\"ahmed@test.com\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void updatingCustomerKeepingOwnEmailIsAllowed() throws Exception {
		long id = idOf(postCustomer("{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\"}"));

		mockMvc.perform(put("/api/customers/" + id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ahmed R.\",\"email\":\"ahmed@test.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ahmed R."));
	}

	@Test
	void notFoundResponseHasErrorMessage() throws Exception {
		mockMvc.perform(get("/api/customers/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Not Found"));
	}

	private ResultActions postCustomer(String body) throws Exception {
		return mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
