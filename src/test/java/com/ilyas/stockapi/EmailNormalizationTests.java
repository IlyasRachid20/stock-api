package com.ilyas.stockapi;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Transactional
class EmailNormalizationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void emailIsTrimmedAndLowerCased() throws Exception {
		postCustomer("{\"name\":\"Ahmed\",\"email\":\"  Ahmed@Test.COM \"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("ahmed@test.com"));
	}

	@Test
	void sameEmailWithDifferentCaseIsADuplicate() throws Exception {
		postCustomer("{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\"}").andExpect(status().isCreated());

		postCustomer("{\"name\":\"Ahmed 2\",\"email\":\"Ahmed@Test.com\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Email ahmed@test.com is already used by another customer"));
	}

	@Test
	void emptyEmailMeansNoEmail() throws Exception {
		postCustomer("{\"name\":\"A\",\"email\":\"\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").doesNotExist());
		postCustomer("{\"name\":\"B\",\"email\":\"   \"}").andExpect(status().isCreated());
	}

	@Test
	void updateKeepingOwnEmailInAnotherCaseIsAllowed() throws Exception {
		String json = postCustomer("{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\"}")
				.andReturn().getResponse().getContentAsString();
		Number id = JsonPath.read(json, "$.id");

		mockMvc.perform(put("/api/customers/" + id).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Ahmed\",\"email\":\"AHMED@test.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ahmed@test.com"));
	}

	@Test
	void nameIsTrimmed() throws Exception {
		postCustomer("{\"name\":\"  Ahmed  \"}").andExpect(jsonPath("$.name").value("Ahmed"));
	}

	private ResultActions postCustomer(String body) throws Exception {
		return mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
