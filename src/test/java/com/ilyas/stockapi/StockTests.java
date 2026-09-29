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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockTests {

	@Autowired
	private MockMvc mockMvc;

	private long productId;
	private long saleId;

	@BeforeEach
	void createProductAndSale() throws Exception {
		long customerId = idOf(postJson("/api/customers", "{\"name\":\"Ahmed\"}"));
		productId = idOf(postJson("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}"));
		saleId = idOf(postJson("/api/sales", "{\"customerId\":" + customerId + "}"));
	}

	@Test
	void sellingTakesQuantityOutOfStock() throws Exception {
		addItem(3, null).andExpect(status().isCreated());

		expectStock(7);
	}

	@Test
	void sellingMoreThanInStockReturns409AndKeepsStock() throws Exception {
		addItem(11, null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Not enough stock for product 'Galaxy S26': 10 available, 11 requested"));

		expectStock(10);
		mockMvc.perform(get("/api/sale-items")).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void sellingExactlyTheWholeStockIsAllowed() throws Exception {
		addItem(10, null).andExpect(status().isCreated());

		expectStock(0);
		addItem(1, null).andExpect(status().isConflict());
	}

	@Test
	void unitPriceDefaultsToProductPrice() throws Exception {
		addItem(1, null)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.unitPrice").value(9500.00));
	}

	@Test
	void givenUnitPriceIsKept() throws Exception {
		addItem(1, "8999.00")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.unitPrice").value(8999.00));
	}

	@Test
	void deletingSaleItemPutsQuantityBackInStock() throws Exception {
		long itemId = idOf(addItem(4, null));
		expectStock(6);

		mockMvc.perform(delete("/api/sale-items/" + itemId)).andExpect(status().isNoContent());

		expectStock(10);
	}

	@Test
	void deletingSalePutsAllItemsBackInStock() throws Exception {
		addItem(2, null).andExpect(status().isCreated());
		addItem(3, null).andExpect(status().isCreated());
		expectStock(5);

		mockMvc.perform(delete("/api/sales/" + saleId)).andExpect(status().isNoContent());

		expectStock(10);
		mockMvc.perform(get("/api/sales/" + saleId)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/sale-items")).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void itemForUnknownProductReturns400() throws Exception {
		postJson("/api/sale-items", "{\"saleId\":" + saleId + ",\"productId\":999999,\"quantity\":1}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Product 999999 does not exist"));
	}

	@Test
	void itemForUnknownSaleReturns400() throws Exception {
		postJson("/api/sale-items", "{\"saleId\":999999,\"productId\":" + productId + ",\"quantity\":1}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Sale 999999 does not exist"));

		expectStock(10);
	}

	@Test
	void saleForUnknownCustomerReturns400() throws Exception {
		postJson("/api/sales", "{\"customerId\":999999}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Customer 999999 does not exist"));
	}

	private ResultActions addItem(int quantity, String unitPrice) throws Exception {
		String price = unitPrice == null ? "" : ",\"unitPrice\":" + unitPrice;
		return postJson("/api/sale-items", "{\"saleId\":" + saleId + ",\"productId\":" + productId + ","
				+ "\"quantity\":" + quantity + price + "}");
	}

	private void expectStock(int quantity) throws Exception {
		mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(quantity));
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
