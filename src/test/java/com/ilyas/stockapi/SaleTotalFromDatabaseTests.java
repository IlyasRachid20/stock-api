package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Not @Transactional: each request commits on its own, like the running app,
 * so the sale's items really are loaded back from the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class SaleTotalFromDatabaseTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SaleItemRepository saleItemRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@AfterEach
	void cleanUp() {
		saleItemRepository.deleteAll();
		saleRepository.deleteAll();
		productRepository.deleteAll();
		customerRepository.deleteAll();
	}

	@Test
	void savedSaleIsReturnedWithItemsTotalAndUpdatedStock() throws Exception {
		long customerId = idOf(postJson("/api/customers", "{\"name\":\"Ahmed\"}"));
		long productId = idOf(postJson("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":10}"));
		long saleId = idOf(postJson("/api/sales", "{\"customerId\":" + customerId + "}"));
		postJson("/api/sale-items", "{\"saleId\":" + saleId + ",\"productId\":" + productId + ",\"quantity\":2}")
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/sales/" + saleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customer.name").value("Ahmed"))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].product.name").value("Galaxy S26"))
				.andExpect(jsonPath("$.total").value(19000.00));

		mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(8));
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
