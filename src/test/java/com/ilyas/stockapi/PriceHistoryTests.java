package com.ilyas.stockapi;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.entity.PriceChange;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.repository.PriceChangeRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Price history and the struck-through previous price. The previous price is the lowest price of
 * the 30 days before a drop, shown during the 30 days after it (European rules for price reductions).
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
@Transactional
class PriceHistoryTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private PriceChangeRepository priceChangeRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void priceDropIsRecordedAndShowsTheOldPriceStruckThrough() throws Exception {
		long id = createProduct("9999.00");

		setPrice(id, "9500.00")
				.andExpect(jsonPath("$.price").value(9500.00))
				.andExpect(jsonPath("$.previousPrice").value(9999.00));

		mockMvc.perform(get("/api/products/" + id + "/price-history"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].oldPrice").value(9999.00))
				.andExpect(jsonPath("$[0].newPrice").value(9500.00))
				.andExpect(jsonPath("$[0].changedBy").value("admin"))
				.andExpect(jsonPath("$[0].changedAt").exists());
	}

	@Test
	void editingOtherFieldsRecordsNoPriceChange() throws Exception {
		long id = createProduct("199.00");

		send(put("/api/products/" + id), "{\"name\":\"USB-C Charger 65W\",\"price\":199.00,\"minQuantity\":4}")
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/products/" + id + "/price-history")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void priceRiseShowsNoReduction() throws Exception {
		long id = createProduct("2590.00");

		setPrice(id, "2690.00").andExpect(jsonPath("$.previousPrice").value(nullValue()));

		mockMvc.perform(get("/api/products/" + id + "/price-history")).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void raisingThePriceJustBeforeAPromotionDoesNotMakeAFakeReduction() throws Exception {
		long id = createProduct("9000.00");
		setPrice(id, "9999.00");

		// 9999 -> 9500 is not a reduction: the product was at 9000 a moment ago
		setPrice(id, "9500.00").andExpect(jsonPath("$.previousPrice").value(nullValue()));
		// Below the lowest recent price, the reduction is shown from that lowest price
		setPrice(id, "8500.00").andExpect(jsonPath("$.previousPrice").value(9000.00));
	}

	@Test
	void pricesOlderThan30DaysDontCount() throws Exception {
		long id = createProduct("9999.00");
		// 40 days ago the product cost 8000 before going up to 9999
		Product product = productRepository.findById(id).orElseThrow();
		PriceChange old = new PriceChange();
		old.setProduct(product);
		old.setOldPrice(new BigDecimal("8000.00"));
		old.setNewPrice(new BigDecimal("9999.00"));
		old.setChangedBy("admin");
		old.setChangedAt(Instant.now().minus(Duration.ofDays(40)));
		priceChangeRepository.save(old);

		setPrice(id, "9500.00").andExpect(jsonPath("$.previousPrice").value(9999.00));
	}

	@Test
	void reductionIsShownFor30Days() throws Exception {
		long id = createProduct("149.00");
		setPrice(id, "129.00");

		reducedDaysAgo(id, 29);
		mockMvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.previousPrice").value(149.00));

		reducedDaysAgo(id, 31);
		mockMvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.previousPrice").value(nullValue()));
	}

	@Test
	void productThatWasNeverSoldIsDeletedWithItsPriceHistory() throws Exception {
		long id = createProduct("49.90");
		setPrice(id, "39.90");

		mockMvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/products/" + id + "/price-history")).andExpect(status().isNotFound());
	}

	private void reducedDaysAgo(long id, int days) {
		Product product = productRepository.findById(id).orElseThrow();
		product.setPriceReducedAt(Instant.now().minus(Duration.ofDays(days)));
		entityManager.flush();
	}

	private long createProduct(String price) throws Exception {
		ResultActions result = send(post("/api/products"), "{\"name\":\"Galaxy S26\",\"price\":" + price + ",\"quantity\":5}")
				.andExpect(status().isCreated());
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

	private ResultActions setPrice(long id, String price) throws Exception {
		return send(put("/api/products/" + id), "{\"name\":\"Galaxy S26\",\"price\":" + price + "}")
				.andExpect(status().isOk());
	}

	private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body)
			throws Exception {
		return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
