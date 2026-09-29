package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Errors raised by Spring MVC itself use the same {"error": "..."} shape as the API's own errors. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ErrorFormatTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createReturns201AndDeleteReturns204() throws Exception {
		String json = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Cable\",\"price\":49.90}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Number id = JsonPath.read(json, "$.id");

		mockMvc.perform(delete("/api/products/" + id))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	void malformedJsonReturns400WithMessage() throws Exception {
		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Cable\",\"price\":\"abc\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Malformed JSON request body"));
	}

	@Test
	void nonNumericIdReturns400WithMessage() throws Exception {
		mockMvc.perform(get("/api/products/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid value 'abc' for parameter 'id'"));
	}

	@Test
	void unknownUrlReturns404WithMessage() throws Exception {
		mockMvc.perform(get("/api/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Not Found"));
	}

	@Test
	void wrongHttpMethodReturns405WithMessage() throws Exception {
		mockMvc.perform(put("/api/sales/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(header().exists("Allow"))
				.andExpect(jsonPath("$.error").value("Method Not Allowed"));
	}

	@Test
	void wrongContentTypeReturns415WithMessage() throws Exception {
		mockMvc.perform(post("/api/products").contentType(MediaType.TEXT_PLAIN).content("Cable"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error").value("Unsupported Media Type"));
	}

	@Test
	void errorsNeverUseSpringsDefaultFormat() throws Exception {
		mockMvc.perform(get("/api/unknown"))
				.andExpect(jsonPath("$.timestamp").doesNotExist())
				.andExpect(jsonPath("$.path").doesNotExist());
	}

}
