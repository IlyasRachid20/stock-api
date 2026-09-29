package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.demo.DemoDataLoader;
import com.ilyas.stockapi.repository.AppUserRepository;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.repository.StockMovementRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Demo mode as used by the public live demo. On H2 this app context has its own database, but in
 * CI on PostgreSQL every test shares one database: so the shop is loaded from an empty state
 * before the tests, and removed after them, to leave nothing behind for the other test classes.
 */
@SpringBootTest(properties = {"app.demo.enabled=true", "app.demo.password=demo-test-password"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoDataTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DemoDataLoader demoDataLoader;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private SaleItemRepository saleItemRepository;

	@Autowired
	private StockMovementRepository stockMovementRepository;

	@BeforeAll
	void loadDemoShopIntoAnEmptyDatabase() throws Exception {
		deleteShopData();
		demoDataLoader.run(null);
	}

	@AfterAll
	void removeDemoShop() {
		deleteShopData();
		userRepository.findByUsername("demo").ifPresent(userRepository::delete);
	}

	private void deleteShopData() {
		stockMovementRepository.deleteAll();
		saleItemRepository.deleteAll();
		saleRepository.deleteAll();
		productRepository.deleteAll();
		customerRepository.deleteAll();
	}

	@Test
	void demoShopIsCreatedWithBothAccounts() {
		assertThat(productRepository.count()).isEqualTo(8);
		assertThat(saleRepository.count()).isEqualTo(4);
		// The admin account must still be created first, even though the demo account exists too
		assertThat(userRepository.findByUsername("admin")).isPresent();
		assertThat(userRepository.findByUsername("demo")).hasValueSatisfying(
				demo -> assertThat(demo.getRole().name()).isEqualTo("CASHIER"));
	}

	@Test
	void runningAgainAddsNothing() throws Exception {
		demoDataLoader.run(null);

		assertThat(productRepository.count()).isEqualTo(8);
		assertThat(saleRepository.count()).isEqualTo(4);
	}

	@Test
	void demoCashierCanLogInAndSeeTheShopButNotChangeStock() throws Exception {
		String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"demo\",\"password\":\"demo-test-password\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String token = "Bearer " + JsonPath.read(json, "$.accessToken");

		// Sales took stock out, so a few products are low: something to show on the low-stock list
		mockMvc.perform(get("/api/products/low-stock").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Phone Case", "AirPods Pro 3", "Screen Protector")));
		mockMvc.perform(get("/api/stock-movements?type=SALE").header("Authorization", token))
				.andExpect(jsonPath("$.page.totalElements").value(8));
		mockMvc.perform(post("/api/products/1/restock").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
				.andExpect(status().isForbidden());
	}

}
