package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.demo.DemoDataLoader;
import com.ilyas.stockapi.dto.CategoryResponse;
import com.ilyas.stockapi.repository.AppUserRepository;
import com.ilyas.stockapi.repository.CategoryRepository;
import com.ilyas.stockapi.repository.PriceChangeRepository;
import com.ilyas.stockapi.repository.ProductImageFileRepository;
import com.ilyas.stockapi.repository.ProductImageRepository;
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

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private com.ilyas.stockapi.repository.OrderStatusChangeRepository orderStatusChangeRepository;

	@Autowired
	private PriceChangeRepository priceChangeRepository;

	@Autowired
	private ProductImageRepository imageRepository;

	@Autowired
	private ProductImageFileRepository imageFileRepository;

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
		orderStatusChangeRepository.deleteAll();
		stockMovementRepository.deleteAll();
		saleItemRepository.deleteAll();
		saleRepository.deleteAll();
		priceChangeRepository.deleteAll();
		imageFileRepository.deleteAll();
		imageRepository.deleteAll();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		customerRepository.deleteAll();
	}

	@Test
	void demoShopIsCreatedWithBothAccounts() {
		assertThat(productRepository.count()).isEqualTo(8);
		assertThat(saleRepository.count()).isBetween(30L, 90L);
		// The admin account must still be created first, even though the demo account exists too
		assertThat(userRepository.findByUsername("admin")).isPresent();
		assertThat(userRepository.findByUsername("demo")).hasValueSatisfying(
				demo -> assertThat(demo.getRole().name()).isEqualTo("CASHIER"));
	}

	@Test
	void everyDemoProductIsInOneOfFourCategories() {
		assertThat(categoryRepository.findAllWithProductCount())
				.extracting(CategoryResponse::name, CategoryResponse::productCount)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("Audio", 1L),
						org.assertj.core.groups.Tuple.tuple("Chargers & cables", 2L),
						org.assertj.core.groups.Tuple.tuple("Phones", 3L),
						org.assertj.core.groups.Tuple.tuple("Protection", 2L));
	}

	@Test
	void twoDemoProductsShowAPriceReductionAndOneARise() {
		java.time.Instant now = java.time.Instant.now();
		java.util.Map<String, java.math.BigDecimal> previous = new java.util.HashMap<>();
		productRepository.findAll().forEach(p -> previous.put(p.getName(), p.getPreviousPriceAt(now)));

		assertThat(previous.get("Galaxy S26")).isEqualByComparingTo("9999.00");
		assertThat(previous.get("Phone Case")).isEqualByComparingTo("149.00");
		assertThat(previous.get("AirPods Pro 3")).isNull();
		assertThat(priceChangeRepository.count()).isEqualTo(3);
		// Sales keep the price of their day: the phone sold at 9999 before the drop, at 9500 after
		Long galaxy = productRepository.findAll().stream().filter(p -> p.getName().equals("Galaxy S26")).findFirst().orElseThrow().getId();
		assertThat(saleItemRepository.findAll()).filteredOn(item -> item.getProduct().getId().equals(galaxy))
				.extracting(item -> item.getUnitPrice().toPlainString())
				.containsOnly("9999.00", "9500.00");
	}

	@Test
	void everyDemoProductHasAPictureAndADescription() {
		assertThat(imageRepository.count()).isEqualTo(8);
		assertThat(imageRepository.findAll()).allMatch(image -> image.getWidth() == 800 && image.getHeight() == 800);
		assertThat(productRepository.findAll()).allMatch(product -> product.getDescription() != null);
	}

	@Test
	void theDemoShopHasOnlineOrdersAtEveryStep() {
		var orders = saleRepository.findAll().stream()
				.filter(sale -> sale.getChannel() == com.ilyas.stockapi.entity.SaleChannel.ONLINE).toList();
		assertThat(orders).hasSizeBetween(8, 14);
		assertThat(orders).extracting(sale -> sale.getStatus().name())
				.contains("NEW", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED");
		// Today's orders wait for a call; none is left waiting long enough to be cancelled automatically
		assertThat(orders).filteredOn(sale -> sale.getStatus() == com.ilyas.stockapi.entity.SaleStatus.NEW)
				.allMatch(sale -> sale.getSaleDate().isAfter(java.time.Instant.now().minus(java.time.Duration.ofHours(30))));
		// Placed in the shop, handled by the staff
		assertThat(orderStatusChangeRepository.findAll()).anyMatch(change -> change.getChangedBy().equals("online shop"))
				.anyMatch(change -> change.getChangedBy().equals("admin"));
	}

	@Test
	void runningAgainAddsNothing() throws Exception {
		long salesBefore = saleRepository.count();
		demoDataLoader.run(null);

		assertThat(productRepository.count()).isEqualTo(8);
		assertThat(categoryRepository.count()).isEqualTo(4);
		assertThat(saleRepository.count()).isEqualTo(salesBefore);
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
				.andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
		mockMvc.perform(get("/api/stock-movements?type=SALE").header("Authorization", token))
				.andExpect(jsonPath("$.page.totalElements", greaterThan(30)));
		mockMvc.perform(post("/api/products/1/restock").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
				.andExpect(status().isForbidden());
	}

	// Sales are spread over the last 30 days, and the history holds a restock and a correction,
	// so the dashboard chart and the stock history look like a real month in the shop
	@Test
	void theDemoMonthIsSpreadOverThirtyDays() {
		var days = saleRepository.findAll().stream()
				.map(sale -> sale.getSaleDate().atZone(java.time.ZoneOffset.UTC).toLocalDate())
				.collect(java.util.stream.Collectors.toSet());
		assertThat(days.size()).isGreaterThanOrEqualTo(25);
		assertThat(saleRepository.findAll()).allMatch(sale -> sale.getSaleDate().isBefore(java.time.Instant.now()));
		// Sale numbers follow the clock: a later sale never has an earlier date
		var byId = saleRepository.findAll(org.springframework.data.domain.Sort.by("id"));
		for (int i = 1; i < byId.size(); i++) {
			assertThat(byId.get(i).getSaleDate()).isAfterOrEqualTo(byId.get(i - 1).getSaleDate());
		}

		var movements = stockMovementRepository.findAll();
		assertThat(movements).anyMatch(m -> m.getType().name().equals("RESTOCK") && "Delivery #1042".equals(m.getReason()));
		assertThat(movements).anyMatch(m -> m.getType().name().equals("ADJUSTMENT") && m.getQuantityChange() == -3);
		// A sale's stock movement carries the same (moved back) date as the sale
		var sale = saleRepository.findAll().get(0);
		var itemIds = saleItemRepository.findBySaleId(sale.getId()).stream().map(i -> i.getId()).toList();
		assertThat(stockMovementRepository.findBySaleItemIdIn(itemIds)).allMatch(m -> m.getCreatedAt().equals(sale.getSaleDate()));
	}

}
