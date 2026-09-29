package com.ilyas.stockapi;

import static org.hamcrest.Matchers.contains;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Stock history, restocks, corrections and low-stock alerts. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "boss", roles = "ADMIN")
@Transactional
class StockHistoryTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void everyStockChangeIsRecordedWithWhoAndTheQuantityAfter() throws Exception {
		long product = createProduct("Galaxy S26", 10, 0);
		long customer = idOf(send(post("/api/customers"), "{\"name\":\"Ahmed\"}"));
		long sale = idOf(send(post("/api/sales"), "{\"customerId\":" + customer + "}"));

		send(post("/api/products/" + product + "/restock"), "{\"quantity\":5,\"reason\":\"Delivery #42\"}").andExpect(status().isOk());
		long item = idOf(send(post("/api/sale-items"), "{\"saleId\":" + sale + ",\"productId\":" + product + ",\"quantity\":3}"));
		send(post("/api/products/" + product + "/adjustments"), "{\"quantityChange\":-1,\"reason\":\"Broken screen\"}").andExpect(status().isOk());
		mockMvc.perform(delete("/api/sale-items/" + item)).andExpect(status().isNoContent());

		// Oldest first here, to read the story in order: 10 → 15 → 12 → 11 → 14
		mockMvc.perform(get("/api/stock-movements?productId=" + product + "&sort=id,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].type", contains("INITIAL", "RESTOCK", "SALE", "ADJUSTMENT", "SALE_CANCELLED")))
				.andExpect(jsonPath("$.content[*].quantityChange", contains(10, 5, -3, -1, 3)))
				.andExpect(jsonPath("$.content[*].quantityAfter", contains(10, 15, 12, 11, 14)))
				.andExpect(jsonPath("$.content[1].reason").value("Delivery #42"))
				.andExpect(jsonPath("$.content[2].saleItemId").value((int) item))
				.andExpect(jsonPath("$.content[3].reason").value("Broken screen"))
				.andExpect(jsonPath("$.content[0].createdBy").value("boss"))
				.andExpect(jsonPath("$.content[0].product.name").value("Galaxy S26"))
				.andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());

		mockMvc.perform(get("/api/products/" + product)).andExpect(jsonPath("$.quantity").value(14));
	}

	@Test
	void historyIsNewestFirstAndCanBeFilteredByType() throws Exception {
		long product = createProduct("Cable", 5, 0);
		send(post("/api/products/" + product + "/restock"), "{\"quantity\":20}");
		send(post("/api/products/" + product + "/restock"), "{\"quantity\":30}");

		mockMvc.perform(get("/api/stock-movements?productId=" + product + "&type=RESTOCK"))
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].quantityChange", contains(30, 20)));
	}

	@Test
	void productUpdateThatChangesTheQuantityIsRecordedAsAnAdjustment() throws Exception {
		long product = createProduct("Charger", 8, 0);

		send(put("/api/products/" + product), "{\"name\":\"Charger\",\"price\":199.00,\"quantity\":6}").andExpect(status().isOk());
		send(put("/api/products/" + product), "{\"name\":\"Charger 2\",\"price\":199.00}").andExpect(status().isOk());

		mockMvc.perform(get("/api/stock-movements?productId=" + product + "&sort=id,asc"))
				.andExpect(jsonPath("$.content[*].type", contains("INITIAL", "ADJUSTMENT")))
				.andExpect(jsonPath("$.content[1].quantityChange").value(-2));
	}

	@Test
	void productCreatedWithoutStockHasNoHistory() throws Exception {
		long product = createProduct("Case", 0, 0);

		mockMvc.perform(get("/api/stock-movements?productId=" + product)).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void adjustmentCannotMakeStockNegative() throws Exception {
		long product = createProduct("Galaxy S26", 2, 0);

		send(post("/api/products/" + product + "/adjustments"), "{\"quantityChange\":-3,\"reason\":\"Count\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Cannot remove 3 from product 'Galaxy S26': only 2 in stock"));
		mockMvc.perform(get("/api/products/" + product)).andExpect(jsonPath("$.quantity").value(2));
	}

	@Test
	void restockAndAdjustmentAreValidated() throws Exception {
		long product = createProduct("Galaxy S26", 2, 0);

		send(post("/api/products/" + product + "/restock"), "{\"quantity\":0}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.quantity").exists());
		send(post("/api/products/" + product + "/adjustments"), "{\"quantityChange\":0,\"reason\":\"\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.reason").exists())
				.andExpect(jsonPath("$.errors.quantityChangeNotZero").value("must not be 0"));
		send(post("/api/products/999999/restock"), "{\"quantity\":5}").andExpect(status().isNotFound());
	}

	@Test
	void lowStockListsProductsAtOrBelowTheirMinimumEmptiestFirst() throws Exception {
		createProduct("Plenty", 50, 10);
		createProduct("At minimum", 10, 10);
		createProduct("Almost gone", 1, 5);
		createProduct("Sold out", 0, 0);

		mockMvc.perform(get("/api/products/low-stock"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].name", contains("Sold out", "Almost gone", "At minimum")))
				.andExpect(jsonPath("$.content[0].lowStock").value(true));

		mockMvc.perform(get("/api/products?search=Plenty"))
				.andExpect(jsonPath("$.content[0].lowStock").value(false))
				.andExpect(jsonPath("$.content[0].minQuantity").value(10));
	}

	@Test
	void restockTakesAProductOffTheLowStockList() throws Exception {
		long product = createProduct("Almost gone", 1, 5);
		send(post("/api/products/" + product + "/restock"), "{\"quantity\":10}");

		mockMvc.perform(get("/api/products/low-stock")).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void minimumLevelCanBeChangedAndStaysWhenLeftOut() throws Exception {
		long product = createProduct("Cable", 5, 2);

		send(put("/api/products/" + product), "{\"name\":\"Cable\",\"price\":49.90,\"minQuantity\":8}")
				.andExpect(jsonPath("$.minQuantity").value(8))
				.andExpect(jsonPath("$.lowStock").value(true));
		send(put("/api/products/" + product), "{\"name\":\"Cable\",\"price\":45.00}")
				.andExpect(jsonPath("$.minQuantity").value(8));
	}

	@Test
	void productThatWasNeverSoldCanStillBeDeletedWithItsHistory() throws Exception {
		long product = createProduct("Mistake", 3, 0);
		send(post("/api/products/" + product + "/restock"), "{\"quantity\":2}");

		mockMvc.perform(delete("/api/products/" + product)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/stock-movements?productId=" + product)).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	@WithMockUser(roles = "CASHIER")
	void cashierCanReadTheHistoryAndLowStockButNotChangeStock() throws Exception {
		mockMvc.perform(get("/api/stock-movements")).andExpect(status().isOk());
		mockMvc.perform(get("/api/products/low-stock")).andExpect(status().isOk());
		send(post("/api/products/1/restock"), "{\"quantity\":5}").andExpect(status().isForbidden());
		send(post("/api/products/1/adjustments"), "{\"quantityChange\":-1,\"reason\":\"x\"}").andExpect(status().isForbidden());
	}

	private long createProduct(String name, int quantity, int minQuantity) throws Exception {
		return idOf(send(post("/api/products"),
				"{\"name\":\"" + name + "\",\"price\":10.00,\"quantity\":" + quantity + ",\"minQuantity\":" + minQuantity + "}")
				.andExpect(status().isCreated()));
	}

	private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
		return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
