package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.repository.StockMovementRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * POST /api/sales with items: all or nothing. Not @Transactional, so each request commits on
 * its own like in the running app, and "nothing saved" really means nothing reached the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "sara", roles = "CASHIER")
class SaleWithItemsTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockMovementRepository stockMovementRepository;

	@Autowired
	private SaleItemRepository saleItemRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CustomerRepository customerRepository;

	private long customer;
	private long phone;
	private long cable;

	// Products are created as an admin; the sales in the tests are made by cashier "sara"
	@BeforeEach
	void createShop() throws Exception {
		customer = idOf(asAdmin("/api/customers", "{\"name\":\"Ahmed\"}"));
		phone = idOf(asAdmin("/api/products", "{\"name\":\"Galaxy S26\",\"price\":9500.00,\"quantity\":5}"));
		cable = idOf(asAdmin("/api/products", "{\"name\":\"USB-C Cable\",\"price\":49.90,\"quantity\":2}"));
	}

	@AfterEach
	void cleanUp() {
		stockMovementRepository.deleteAll();
		saleItemRepository.deleteAll();
		saleRepository.deleteAll();
		productRepository.deleteAll();
		customerRepository.deleteAll();
	}

	@Test
	void saleWithItemsIsCreatedInOneRequest() throws Exception {
		postJson("/api/sales", "{\"customerId\":" + customer + ",\"items\":["
				+ "{\"productId\":" + phone + ",\"quantity\":1},"
				+ "{\"productId\":" + cable + ",\"quantity\":2,\"unitPrice\":45.00}]}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.total").value(9590.00));

		assertStock(phone, 4);
		assertStock(cable, 0);
		mockMvc.perform(get("/api/stock-movements?type=SALE"))
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].createdBy").value("sara"));
	}

	@Test
	void ifOneItemIsShortOfStockNothingIsSaved() throws Exception {
		postJson("/api/sales", "{\"customerId\":" + customer + ",\"items\":["
				+ "{\"productId\":" + phone + ",\"quantity\":1},"
				+ "{\"productId\":" + cable + ",\"quantity\":3}]}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Not enough stock for product 'USB-C Cable': 2 available, 3 requested"));

		// The phone line came first and succeeded, but was rolled back with the rest
		assertThat(saleRepository.count()).isZero();
		assertThat(saleItemRepository.count()).isZero();
		assertStock(phone, 5);
		assertStock(cable, 2);
		assertThat(stockMovementRepository.findAll()).noneMatch(m -> m.getType().name().equals("SALE"));
	}

	@Test
	void unknownProductInItemsSavesNothing() throws Exception {
		postJson("/api/sales", "{\"customerId\":" + customer + ",\"items\":[{\"productId\":999999,\"quantity\":1}]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Product 999999 does not exist"));

		assertThat(saleRepository.count()).isZero();
	}

	@Test
	void sameProductTwiceIsCheckedAgainstTheStockLeft() throws Exception {
		postJson("/api/sales", "{\"customerId\":" + customer + ",\"items\":["
				+ "{\"productId\":" + cable + ",\"quantity\":2},"
				+ "{\"productId\":" + cable + ",\"quantity\":1}]}")
				.andExpect(status().isConflict());

		assertStock(cable, 2);
	}

	@Test
	void itemsAreValidated() throws Exception {
		postJson("/api/sales", "{\"customerId\":" + customer + ",\"items\":[{\"productId\":null,\"quantity\":0}]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors['items[0].productId']").exists())
				.andExpect(jsonPath("$.errors['items[0].quantity']").exists());
	}

	@Test
	void saleWithoutItemsStillWorks() throws Exception {
		postJson("/api/sales", "{\"customerId\":" + customer + "}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.items.length()").value(0));
	}

	private void assertStock(long productId, int quantity) throws Exception {
		mockMvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.quantity").value(quantity));
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions asAdmin(String path, String body) throws Exception {
		return mockMvc.perform(post(path).with(user("admin").roles("ADMIN"))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
