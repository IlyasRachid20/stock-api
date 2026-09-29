package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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
class DeleteInUseTests {

	@Autowired
	private MockMvc mockMvc;

	private long customerId;
	private long productId;
	private long saleId;

	@BeforeEach
	void createSaleWithOneItem() throws Exception {
		customerId = idOf(postJson("/api/customers", "{\"name\":\"Ahmed\"}"));
		productId = idOf(postJson("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}"));
		saleId = idOf(postJson("/api/sales", "{\"customerId\":" + customerId + "}"));
		postJson("/api/sale-items", "{\"saleId\":" + saleId + ",\"productId\":" + productId + ",\"quantity\":1}");
	}

	@Test
	void productThatWasSoldCannotBeDeleted() throws Exception {
		mockMvc.perform(delete("/api/products/" + productId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Product 'Galaxy S26' cannot be deleted: it appears in 1 sale item(s)"));

		mockMvc.perform(get("/api/products/" + productId)).andExpect(status().isOk());
	}

	@Test
	void customerWithSalesCannotBeDeleted() throws Exception {
		mockMvc.perform(delete("/api/customers/" + customerId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Customer 'Ahmed' cannot be deleted: they have 1 sale(s)"));

		mockMvc.perform(get("/api/customers/" + customerId)).andExpect(status().isOk());
	}

	@Test
	void productAndCustomerCanBeDeletedOnceTheirSalesAreGone() throws Exception {
		mockMvc.perform(delete("/api/sales/" + saleId)).andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/products/" + productId)).andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/customers/" + customerId)).andExpect(status().isNoContent());
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
