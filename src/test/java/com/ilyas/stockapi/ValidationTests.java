package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValidationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Test
	void validProductIsCreated() throws Exception {
		postJson("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"name\":\"\",\"price\":10.00,\"quantity\":1}",
			"{\"price\":10.00,\"quantity\":1}" })
	void productWithoutNameIsRejected(String body) throws Exception {
		postJson("/api/products", body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void productWithNegativePriceIsRejected() throws Exception {
		postJson("/api/products", "{\"name\":\"Cable\",\"price\":-5,\"quantity\":1}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.price").exists());
	}

	@Test
	void productWithoutPriceIsRejected() throws Exception {
		postJson("/api/products", "{\"name\":\"Cable\",\"quantity\":1}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.price").exists());
	}

	@Test
	void productWithNegativeQuantityIsRejected() throws Exception {
		postJson("/api/products", "{\"name\":\"Cable\",\"price\":10.00,\"quantity\":-1}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.quantity").exists());
	}

	@Test
	void rejectedProductIsNotSaved() throws Exception {
		postJson("/api/products", "{\"name\":\"\",\"price\":-5,\"quantity\":-1}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.price").exists())
				.andExpect(jsonPath("$.errors.quantity").exists());

		mockMvc.perform(get("/api/products")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void invalidUpdateIsRejected() throws Exception {
		Product product = new Product();
		product.setName("Galaxy S26");
		product.setPrice(new BigDecimal("9500.00"));
		Long id = productRepository.save(product).getId();

		mockMvc.perform(put("/api/products/" + id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Galaxy S26\",\"price\":-1,\"quantity\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.price").exists());
	}

	@Test
	void customerWithoutNameIsRejected() throws Exception {
		postJson("/api/customers", "{\"email\":\"ahmed@test.com\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void customerWithInvalidEmailIsRejected() throws Exception {
		postJson("/api/customers", "{\"name\":\"Ahmed\",\"email\":\"not-an-email\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists());
	}

	@Test
	void saleWithoutCustomerIsRejected() throws Exception {
		postJson("/api/sales", "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.customerId").exists());
	}

	@Test
	void saleItemWithZeroQuantityIsRejected() throws Exception {
		postJson("/api/sale-items", "{\"quantity\":0,\"unitPrice\":10.00}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.quantity").exists())
				.andExpect(jsonPath("$.errors.saleId").exists())
				.andExpect(jsonPath("$.errors.productId").exists());
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
