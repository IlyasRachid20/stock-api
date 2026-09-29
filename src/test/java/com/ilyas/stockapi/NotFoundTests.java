package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotFoundTests {

	private static final long MISSING_ID = 999_999L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@ParameterizedTest
	@ValueSource(strings = { "/api/customers", "/api/products", "/api/sales", "/api/sale-items" })
	void getMissingIdReturns404(String path) throws Exception {
		mockMvc.perform(get(path + "/" + MISSING_ID)).andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/customers", "/api/products", "/api/sales", "/api/sale-items" })
	void deleteMissingIdReturns404(String path) throws Exception {
		mockMvc.perform(delete(path + "/" + MISSING_ID)).andExpect(status().isNotFound());
	}

	@Test
	void updateMissingProductReturns404() throws Exception {
		mockMvc.perform(put("/api/products/" + MISSING_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ghost\",\"price\":1.00,\"quantity\":1}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/products")).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void updateMissingCustomerReturns404() throws Exception {
		mockMvc.perform(put("/api/customers/" + MISSING_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ghost\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getExistingProductReturns200() throws Exception {
		Product product = new Product();
		product.setName("Galaxy S26");
		product.setPrice(new BigDecimal("9500.00"));
		product.setQuantity(10);
		Long id = productRepository.save(product).getId();

		mockMvc.perform(get("/api/products/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Galaxy S26"));
	}

}
