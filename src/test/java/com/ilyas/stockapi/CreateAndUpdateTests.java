package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class CreateAndUpdateTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createdCustomerCanBeFetched() throws Exception {
		long id = createAndGetId("/api/customers",
				"{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\",\"phone\":\"0600000000\"}");

		mockMvc.perform(get("/api/customers/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ahmed"))
				.andExpect(jsonPath("$.email").value("ahmed@test.com"))
				.andExpect(jsonPath("$.phone").value("0600000000"));
	}

	@Test
	void createdProductAppearsInList() throws Exception {
		createAndGetId("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}");

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].name").value("Galaxy S26"))
				.andExpect(jsonPath("$.content[0].price").value(9500.00))
				.andExpect(jsonPath("$.content[0].quantity").value(10));
	}

	@Test
	void productIsUpdated() throws Exception {
		long id = createAndGetId("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}");

		mockMvc.perform(put("/api/products/" + id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Galaxy S26\",\"price\":8999.00,\"quantity\":7}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.price").value(8999.00))
				.andExpect(jsonPath("$.quantity").value(7));

		mockMvc.perform(get("/api/products")).andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void saleAndSaleItemAreCreatedForExistingCustomerAndProduct() throws Exception {
		long customerId = createAndGetId("/api/customers", "{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\"}");
		long productId = createAndGetId("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}");

		long saleId = createAndGetId("/api/sales", "{\"customerId\":" + customerId + "}");

		mockMvc.perform(get("/api/sales/" + saleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customer.id").value(customerId))
				.andExpect(jsonPath("$.saleDate").isNotEmpty());

		long itemId = createAndGetId("/api/sale-items",
				"{\"saleId\":" + saleId + ",\"productId\":" + productId + ","
						+ "\"quantity\":2,\"unitPrice\":9500.00}");

		mockMvc.perform(get("/api/sale-items/" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saleId").value(saleId))
				.andExpect(jsonPath("$.product.id").value(productId))
				.andExpect(jsonPath("$.quantity").value(2))
				.andExpect(jsonPath("$.unitPrice").value(9500.00));
	}

	@Test
	void deletedCustomerIsGone() throws Exception {
		long id = createAndGetId("/api/customers", "{\"name\":\"Ahmed\"}");

		mockMvc.perform(delete("/api/customers/" + id)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/customers/" + id)).andExpect(status().isNotFound());
	}

	private long createAndGetId(String path, String body) throws Exception {
		ResultActions result = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber());
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
