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
class SaleTotalTests {

	@Autowired
	private MockMvc mockMvc;

	private long phoneId;
	private long cableId;
	private long saleId;

	@BeforeEach
	void createProductsAndSale() throws Exception {
		long customerId = idOf(postJson("/api/customers", "{\"name\":\"Ahmed\"}"));
		phoneId = idOf(postJson("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}"));
		cableId = idOf(postJson("/api/products", "{\"name\":\"Cable\",\"price\":49.90,\"quantity\":100}"));
		saleId = idOf(postJson("/api/sales", "{\"customerId\":" + customerId + "}"));
	}

	@Test
	void newSaleHasNoItemsAndZeroTotal() throws Exception {
		mockMvc.perform(get("/api/sales/" + saleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0))
				.andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void saleShowsItsItemsAndTotal() throws Exception {
		addItem(phoneId, 2);
		addItem(cableId, 3);

		mockMvc.perform(get("/api/sales/" + saleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].product.name").value("Galaxy S26"))
				.andExpect(jsonPath("$.items[0].quantity").value(2))
				.andExpect(jsonPath("$.items[0].sale").doesNotExist())
				.andExpect(jsonPath("$.items[1].product.name").value("Cable"))
				.andExpect(jsonPath("$.total").value(19149.70));
	}

	@Test
	void totalIsUpdatedWhenItemIsDeleted() throws Exception {
		addItem(phoneId, 2);
		long cableItemId = idOf(addItem(cableId, 3));

		mockMvc.perform(delete("/api/sale-items/" + cableItemId)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/sales/" + saleId))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.total").value(19000.00));
	}

	@Test
	void saleItemShowsItsSaleIdAndLineTotal() throws Exception {
		long itemId = idOf(addItem(cableId, 3));

		mockMvc.perform(get("/api/sale-items/" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saleId").value(saleId))
				.andExpect(jsonPath("$.sale").doesNotExist())
				.andExpect(jsonPath("$.lineTotal").value(149.70));
	}

	@Test
	void itemsAndTotalSentWhenCreatingSaleAreIgnored() throws Exception {
		long customerId = idOf(postJson("/api/customers", "{\"name\":\"Sara\"}"));

		postJson("/api/sales", "{\"customerId\":" + customerId + ",\"total\":999,\"items\":[{\"quantity\":5}]}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.items.length()").value(0))
				.andExpect(jsonPath("$.total").value(0));
	}

	private ResultActions addItem(long productId, int quantity) throws Exception {
		return postJson("/api/sale-items", "{\"saleId\":" + saleId + ",\"productId\":" + productId + ","
				+ "\"quantity\":" + quantity + "}")
				.andExpect(status().isCreated());
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
