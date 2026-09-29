package com.ilyas.stockapi;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PaginationAndSearchTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void productsArePaginated() throws Exception {
		for (int i = 1; i <= 25; i++) {
			createProduct("Product " + i, "10.00");
		}

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(20))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.totalElements").value(25))
				.andExpect(jsonPath("$.page.totalPages").value(2));

		mockMvc.perform(get("/api/products?page=1&size=20"))
				.andExpect(jsonPath("$.content.length()").value(5))
				.andExpect(jsonPath("$.content[0].name").value("Product 21"));
	}

	@Test
	void pageSizeIsCappedAt100() throws Exception {
		mockMvc.perform(get("/api/products?size=5000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(100));
	}

	@Test
	void productSearchIsCaseInsensitiveAndMatchesPartOfTheName() throws Exception {
		createProduct("Galaxy S26", "9500.00");
		createProduct("Galaxy Buds", "1200.00");
		createProduct("iPhone 17", "12000.00");

		mockMvc.perform(get("/api/products?search=gALaxy"))
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].name", contains("Galaxy S26", "Galaxy Buds")));

		mockMvc.perform(get("/api/products?search=phone"))
				.andExpect(jsonPath("$.content[*].name", contains("iPhone 17")));
	}

	@Test
	void productsCanBeSortedByPrice() throws Exception {
		createProduct("Cable", "49.90");
		createProduct("Galaxy S26", "9500.00");
		createProduct("Charger", "199.00");

		mockMvc.perform(get("/api/products?sort=price,desc"))
				.andExpect(jsonPath("$.content[*].name", contains("Galaxy S26", "Charger", "Cable")));
	}

	@Test
	void sortingByUnknownFieldReturns400() throws Exception {
		mockMvc.perform(get("/api/products?sort=color"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Cannot sort by unknown field 'color'"));
	}

	@Test
	void customerSearchMatchesNameOrEmail() throws Exception {
		postJson("/api/customers", "{\"name\":\"Ahmed\",\"email\":\"ahmed@test.com\"}");
		postJson("/api/customers", "{\"name\":\"Sara\",\"email\":\"sara@shop.ma\"}");
		postJson("/api/customers", "{\"name\":\"Youssef\"}");

		mockMvc.perform(get("/api/customers?search=AHM"))
				.andExpect(jsonPath("$.content[*].name", contains("Ahmed")));

		mockMvc.perform(get("/api/customers?search=shop.ma"))
				.andExpect(jsonPath("$.content[*].name", contains("Sara")));
	}

	@Test
	void salesCanBeFilteredByCustomerAndAreNewestFirst() throws Exception {
		long ahmed = idOf(postJson("/api/customers", "{\"name\":\"Ahmed\"}"));
		long sara = idOf(postJson("/api/customers", "{\"name\":\"Sara\"}"));
		long first = idOf(postJson("/api/sales", "{\"customerId\":" + ahmed + "}"));
		idOf(postJson("/api/sales", "{\"customerId\":" + sara + "}"));
		long last = idOf(postJson("/api/sales", "{\"customerId\":" + ahmed + "}"));

		mockMvc.perform(get("/api/sales?customerId=" + ahmed))
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].id", contains((int) last, (int) first)));
	}

	@Test
	void saleItemsCanBeFilteredBySale() throws Exception {
		long customer = idOf(postJson("/api/customers", "{\"name\":\"Ahmed\"}"));
		long product = idOf(createProduct("Galaxy S26", "9500.00"));
		long saleA = idOf(postJson("/api/sales", "{\"customerId\":" + customer + "}"));
		long saleB = idOf(postJson("/api/sales", "{\"customerId\":" + customer + "}"));
		postJson("/api/sale-items", "{\"saleId\":" + saleA + ",\"productId\":" + product + ",\"quantity\":1}");
		postJson("/api/sale-items", "{\"saleId\":" + saleB + ",\"productId\":" + product + ",\"quantity\":2}");

		mockMvc.perform(get("/api/sale-items?saleId=" + saleB))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].quantity").value(2));
	}

	private ResultActions createProduct(String name, String price) throws Exception {
		return postJson("/api/products", "{\"name\":\"" + name + "\",\"price\":" + price + ",\"quantity\":10}");
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
