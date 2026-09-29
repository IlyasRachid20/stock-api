package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDocsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void openApiDescriptionListsAllEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Stock API"))
				.andExpect(jsonPath("$.paths['/api/customers']").exists())
				.andExpect(jsonPath("$.paths['/api/products/{id}']").exists())
				.andExpect(jsonPath("$.paths['/api/sales']").exists())
				.andExpect(jsonPath("$.paths['/api/sale-items/{id}']").exists());
	}

	@Test
	void swaggerUiPageIsServed() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

}
