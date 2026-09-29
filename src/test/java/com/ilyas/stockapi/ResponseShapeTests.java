package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Checks exactly which fields each response exposes, so internal entity fields can't leak into the API. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResponseShapeTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void responsesExposeOnlyTheirDocumentedFields() throws Exception {
		long customerId = idOf(postJson("/api/customers", "{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\",\"phone\":\"0600000000\"}"));
		long productId = idOf(postJson("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}"));
		long saleId = idOf(postJson("/api/sales", "{\"customerId\":" + customerId + "}"));
		long itemId = idOf(postJson("/api/sale-items", "{\"saleId\":" + saleId + ",\"productId\":" + productId + ",\"quantity\":2}"));

		expectFields("/api/customers/" + customerId, "$", "id", "name", "email", "phone");
		expectFields("/api/products/" + productId, "$", "id", "name", "price", "quantity");
		expectFields("/api/sale-items/" + itemId, "$", "id", "saleId", "product", "quantity", "unitPrice", "lineTotal");
		expectFields("/api/sales/" + saleId, "$", "id", "customer", "saleDate", "items", "total");
		expectFields("/api/sales/" + saleId, "$.customer", "id", "name");
		expectFields("/api/sales/" + saleId, "$.items[0].product", "id", "name");

		mockMvc.perform(get("/api/sales/" + saleId)).andExpect(jsonPath("$.items[0].lineTotal").value(19000.00));
	}

	@Test
	void productCreatedWithoutQuantityStartsAtZero() throws Exception {
		postJson("/api/products", "{\"name\":\"Cable\",\"price\":49.90}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.quantity").value(0));
	}

	@Test
	void updatingProductWithoutQuantityKeepsStock() throws Exception {
		long id = idOf(postJson("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}"));

		mockMvc.perform(put("/api/products/" + id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Galaxy S26 Ultra\",\"price\":11000.00}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Galaxy S26 Ultra"))
				.andExpect(jsonPath("$.quantity").value(10));
	}

	@Test
	void updatingCustomerKeepsItsId() throws Exception {
		long id = idOf(postJson("/api/customers", "{\"name\":\"Ahmed\"}"));

		mockMvc.perform(put("/api/customers/" + id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":12345,\"name\":\"Ahmed R.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id));
	}

	// Asserts the JSON object at jsonPath in GET path has exactly these fields, no more and no less
	private void expectFields(String path, String jsonPath, String... fields) throws Exception {
		String json = mockMvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		Map<String, Object> object = JsonPath.read(json, jsonPath);
		assertThat(object.keySet()).as(path + " " + jsonPath).containsExactlyInAnyOrder(fields);
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
