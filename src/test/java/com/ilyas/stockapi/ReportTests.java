package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.entity.Customer;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleItem;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reports in the shop's time zone. Morocco is UTC+1 in September 2026, so the sale at
 * 2026-09-10T23:30Z belongs to September 11 locally.
 */
@SpringBootTest(properties = "app.time-zone=Africa/Casablanca")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Transactional
class ReportTests {

	private static final String RANGE = "from=2026-09-09&to=2026-09-12";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private SaleItemRepository saleItemRepository;

	// Sep 10 (local): Ahmed buys 1 phone (9500) + 2 cables (2 x 49.90)
	// Sep 11 (local, 23:30 UTC on Sep 10): Sara buys 3 cables (3 x 49.90)
	// Sep 12: an empty sale (no items): not counted
	// Sep 20: outside the range
	@BeforeEach
	void createSales() {
		Customer ahmed = customer("Ahmed");
		Customer sara = customer("Sara, \"VIP\"");
		Product phone = product("Galaxy S26", "9500.00");
		Product cable = product("USB-C Cable", "49.90");

		Sale first = sale(ahmed, "2026-09-10T09:00:00Z");
		item(first, phone, 1);
		item(first, cable, 2);
		Sale lateNight = sale(sara, "2026-09-10T23:30:00Z");
		item(lateNight, cable, 3);
		sale(ahmed, "2026-09-12T10:00:00Z");
		Sale outside = sale(ahmed, "2026-09-20T10:00:00Z");
		item(outside, phone, 5);
	}

	@Test
	void summaryAddsUpTheRange() throws Exception {
		mockMvc.perform(get("/api/reports/summary?" + RANGE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.from").value("2026-09-09"))
				.andExpect(jsonPath("$.to").value("2026-09-12"))
				.andExpect(jsonPath("$.salesCount").value(2))
				.andExpect(jsonPath("$.itemsSold").value(6))
				.andExpect(jsonPath("$.revenue").value(9749.50))
				.andExpect(jsonPath("$.averageSale").value(4874.75));
	}

	@Test
	void salesByDayHasEveryDayAndUsesTheShopsTimeZone() throws Exception {
		mockMvc.perform(get("/api/reports/sales-by-day?" + RANGE))
				.andExpect(jsonPath("$[*].date", contains("2026-09-09", "2026-09-10", "2026-09-11", "2026-09-12")))
				.andExpect(jsonPath("$[*].salesCount", contains(0, 1, 1, 0)))
				.andExpect(jsonPath("$[*].itemsSold", contains(0, 3, 3, 0)))
				.andExpect(jsonPath("$[1].revenue").value(9599.80))
				.andExpect(jsonPath("$[2].revenue").value(149.70))
				.andExpect(jsonPath("$[0].revenue").value(0));
	}

	@Test
	void topProductsAreRankedByQuantitySold() throws Exception {
		mockMvc.perform(get("/api/reports/top-products?" + RANGE + "&limit=5"))
				.andExpect(jsonPath("$[*].name", contains("USB-C Cable", "Galaxy S26")))
				.andExpect(jsonPath("$[0].quantitySold").value(5))
				.andExpect(jsonPath("$[0].revenue").value(249.50))
				.andExpect(jsonPath("$[1].quantitySold").value(1));

		mockMvc.perform(get("/api/reports/top-products?" + RANGE + "&limit=1"))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void csvExportHasOneRowPerLineInLocalTimeAndEscapesValues() throws Exception {
		String csv = mockMvc.perform(get("/api/reports/sales.csv?" + RANGE))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"sales-2026-09-09_2026-09-12.csv\""))
				.andReturn().getResponse().getContentAsString();

		assertThat(csv.lines()).containsExactly(
				"date,sale_id,customer,product,quantity,unit_price,line_total",
				"2026-09-10 10:00:00," + saleIdAt("2026-09-10T09:00:00Z") + ",Ahmed,Galaxy S26,1,9500.00,9500.00",
				"2026-09-10 10:00:00," + saleIdAt("2026-09-10T09:00:00Z") + ",Ahmed,USB-C Cable,2,49.90,99.80",
				"2026-09-11 00:30:00," + saleIdAt("2026-09-10T23:30:00Z") + ",\"Sara, \"\"VIP\"\"\",USB-C Cable,3,49.90,149.70");
	}

	@Test
	void csvNeutralizesSpreadsheetFormulas() throws Exception {
		Sale sale = sale(customer("=HYPERLINK(\"http://evil.example\")"), "2026-09-11T08:00:00Z");
		item(sale, product("+Cable", "10.00"), 1);

		String csv = mockMvc.perform(get("/api/reports/sales.csv?" + RANGE)).andReturn().getResponse().getContentAsString();

		assertThat(csv).contains("\"'=HYPERLINK(\"\"http://evil.example\"\")\"", ",'+Cable,");
	}

	@Test
	void invalidRangesAreRejected() throws Exception {
		mockMvc.perform(get("/api/reports/summary?from=2026-09-12&to=2026-09-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("from must be on or before to"));
		mockMvc.perform(get("/api/reports/summary?from=2024-01-01&to=2026-09-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("The range can't be longer than 366 days"));
		mockMvc.perform(get("/api/reports/summary?from=not-a-date"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/top-products?limit=0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("limit must be between 1 and 50"));
	}

	@Test
	void withoutDatesTheLast30DaysAreUsed() throws Exception {
		mockMvc.perform(get("/api/reports/sales-by-day")).andExpect(jsonPath("$.length()").value(30));
	}

	@Test
	@WithMockUser(roles = "CASHIER")
	void cashierCannotSeeReports() throws Exception {
		mockMvc.perform(get("/api/reports/summary")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/reports/sales.csv")).andExpect(status().isForbidden());
	}

	private Customer customer(String name) {
		Customer customer = new Customer();
		customer.setName(name);
		return customerRepository.save(customer);
	}

	private Product product(String name, String price) {
		Product product = new Product();
		product.setName(name);
		product.setPrice(new BigDecimal(price));
		product.setQuantity(100);
		return productRepository.save(product);
	}

	private Sale sale(Customer customer, String instant) {
		Sale sale = new Sale();
		sale.setCustomer(customer);
		sale.setSaleDate(Instant.parse(instant));
		return saleRepository.save(sale);
	}

	private void item(Sale sale, Product product, int quantity) {
		SaleItem item = new SaleItem();
		item.setSale(sale);
		item.setProduct(product);
		item.setQuantity(quantity);
		item.setUnitPrice(product.getPrice());
		saleItemRepository.save(item);
	}

	private long saleIdAt(String instant) {
		return saleRepository.findAll().stream()
				.filter(sale -> sale.getSaleDate().equals(Instant.parse(instant)))
				.findFirst().orElseThrow().getId();
	}

}
