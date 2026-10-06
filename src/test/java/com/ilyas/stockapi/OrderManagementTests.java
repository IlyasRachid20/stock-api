package com.ilyas.stockapi;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.entity.Customer;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.shop.UnconfirmedOrderCanceller;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Online orders handled by the staff: a cashier ("sara") confirms, ships, delivers, cancels.
 * Orders are placed through the shop by anonymous visitors.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "sara", roles = "CASHIER")
@Transactional
class OrderManagementTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private UnconfirmedOrderCanceller canceller;

	@Autowired
	private EntityManager entityManager;

	private long cable;

	@BeforeEach
	void createProduct() {
		Product product = new Product();
		product.setName("USB-C Cable Managed");
		product.setPrice(new BigDecimal("49.90"));
		product.setQuantity(50);
		cable = productRepository.save(product).getId();
	}

	@Test
	void anOrderGoesFromNewToDeliveredWithWhoDidWhat() throws Exception {
		long id = order("Imane Alaoui", "0611223344", 2);

		changeStatus(id, "CONFIRMED", "Called at 10:00")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.nextStatuses", contains("SHIPPED", "CANCELLED")));
		changeStatus(id, "SHIPPED", null).andExpect(status().isOk());
		changeStatus(id, "DELIVERED", null)
				.andExpect(jsonPath("$.nextStatuses", empty()))
				.andExpect(jsonPath("$.history[*].status", contains("NEW", "CONFIRMED", "SHIPPED", "DELIVERED")))
				.andExpect(jsonPath("$.history[0].changedBy").value("online shop"))
				.andExpect(jsonPath("$.history[1].changedBy").value("sara"))
				.andExpect(jsonPath("$.history[1].note").value("Called at 10:00"))
				.andExpect(jsonPath("$.delivery.phone").value("+212611223344"))
				.andExpect(jsonPath("$.total").value(129.80));
	}

	@Test
	void aStepCantBeSkipped() throws Exception {
		long id = order("Imane Alaoui", "0611223344", 1);
		String number = saleRepository.findById(id).orElseThrow().getOrderNumber();

		changeStatus(id, "SHIPPED", null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Order " + number + " is new and can't become shipped"));
	}

	@Test
	void cancellingPutsTheStockBackAndKeepsTheOrder() throws Exception {
		long id = order("Imane Alaoui", "0611223344", 2);
		mockMvc.perform(get("/api/products/" + cable)).andExpect(jsonPath("$.quantity").value(48));

		changeStatus(id, "CANCELLED", "Customer changed their mind")
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.items.length()").value(1));

		mockMvc.perform(get("/api/products/" + cable)).andExpect(jsonPath("$.quantity").value(50));
		String number = saleRepository.findById(id).orElseThrow().getOrderNumber();
		mockMvc.perform(get("/api/stock-movements?productId=" + cable))
				.andExpect(jsonPath("$.content[0].type").value("SALE_CANCELLED"))
				.andExpect(jsonPath("$.content[0].reason").value("Order " + number + " cancelled"))
				.andExpect(jsonPath("$.content[0].createdBy").value("sara"));
		// Cancelled once: a second cancel is refused, the stock isn't put back twice
		changeStatus(id, "CANCELLED", null).andExpect(status().isConflict());
		mockMvc.perform(get("/api/products/" + cable)).andExpect(jsonPath("$.quantity").value(50));
	}

	@Test
	void aParcelRefusedAtTheDoorGoesBackInStock() throws Exception {
		long id = order("Imane Alaoui", "0611223344", 3);
		changeStatus(id, "CONFIRMED", null);
		changeStatus(id, "SHIPPED", null);

		changeStatus(id, "RETURNED", "Refused at the door").andExpect(jsonPath("$.status").value("RETURNED"));

		mockMvc.perform(get("/api/products/" + cable)).andExpect(jsonPath("$.quantity").value(50));
	}

	@Test
	void ordersAreFilteredByStatusAndFoundByNumberNameOrPhone() throws Exception {
		long imane = order("Imane Alaoui", "0611223344", 1);
		long hamza = order("Hamza Bennani", "0622334455", 1);
		changeStatus(imane, "CONFIRMED", null);
		String number = saleRepository.findById(hamza).orElseThrow().getOrderNumber();

		mockMvc.perform(get("/api/orders?status=NEW"))
				.andExpect(jsonPath("$.content[*].id", contains((int) hamza)));
		mockMvc.perform(get("/api/orders?search=hamza")).andExpect(jsonPath("$.content[*].id", contains((int) hamza)));
		mockMvc.perform(get("/api/orders?search=06 22 33")).andExpect(jsonPath("$.content[*].id", contains((int) hamza)));
		mockMvc.perform(get("/api/orders").param("search", number.toLowerCase()))
				.andExpect(jsonPath("$.content[*].id", contains((int) hamza)))
				.andExpect(jsonPath("$.content[0].customerName").value("Hamza Bennani"))
				.andExpect(jsonPath("$.content[0].city").value("Casablanca"));

		mockMvc.perform(get("/api/orders/counts"))
				.andExpect(jsonPath("$.NEW").value(1))
				.andExpect(jsonPath("$.CONFIRMED").value(1))
				.andExpect(jsonPath("$.SHIPPED").value(0));
	}

	@Test
	void aCounterSaleIsNotAnOnlineOrder() throws Exception {
		Customer customer = new Customer();
		customer.setName("Walk-in");
		Sale sale = new Sale();
		sale.setCustomer(customerRepository.save(customer));
		long id = saleRepository.save(sale).getId();

		mockMvc.perform(get("/api/orders/" + id)).andExpect(status().isNotFound());
		changeStatus(id, "CONFIRMED", null).andExpect(status().isNotFound());
	}

	@Test
	void ordersNobodyConfirmedIn48HoursAreCancelledByTheSystem() throws Exception {
		long stale = order("Imane Alaoui", "0611223344", 2);
		long recent = order("Hamza Bennani", "0622334455", 1);
		saleRepository.findById(stale).orElseThrow().setSaleDate(Instant.now().minus(Duration.ofHours(49)));
		entityManager.flush();

		// The hourly job runs without a logged-in user
		SecurityContextHolder.clearContext();
		canceller.cancelStaleOrders();
		SecurityContextHolder.getContext().setAuthentication(
				org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
						"sara", null, org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_CASHIER")));

		mockMvc.perform(get("/api/orders/" + stale))
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.history[1].changedBy").value("system"))
				.andExpect(jsonPath("$.history[1].note").value("Not confirmed within 48 hours"));
		mockMvc.perform(get("/api/orders/" + recent)).andExpect(jsonPath("$.status").value("NEW"));
		// 50 - 2 - 1 + 2: the stale order's cables are back
		mockMvc.perform(get("/api/products/" + cable)).andExpect(jsonPath("$.quantity").value(49));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void onlyDeliveredOrdersAddToTheOnlineRevenue() throws Exception {
		long id = order("Imane Alaoui", "0611223344", 10);
		mockMvc.perform(get("/api/reports/summary")).andExpect(jsonPath("$.onlineSalesCount").value(0));

		changeStatus(id, "CONFIRMED", null);
		changeStatus(id, "SHIPPED", null);
		changeStatus(id, "DELIVERED", null);

		mockMvc.perform(get("/api/reports/summary"))
				.andExpect(jsonPath("$.salesCount").value(1))
				.andExpect(jsonPath("$.onlineSalesCount").value(1))
				.andExpect(jsonPath("$.onlineRevenue").value(499.00))
				.andExpect(jsonPath("$.revenue").value(499.00));
	}

	private long order(String name, String phone, int quantity) throws Exception {
		String json = mockMvc.perform(post("/api/shop/orders").with(anonymous()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\",\"city\":\""
								+ (name.startsWith("Hamza") ? "Casablanca" : "Rabat") + "\",\"address\":\"1 rue X\","
								+ "\"items\":[{\"productId\":" + cable + ",\"quantity\":" + quantity + "}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String number = JsonPath.read(json, "$.orderNumber");
		return saleRepository.findByOrderNumber(number).orElseThrow().getId();
	}

	private ResultActions changeStatus(long id, String status, String note) throws Exception {
		return mockMvc.perform(post("/api/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"" + status + "\"" + (note == null ? "" : ",\"note\":\"" + note + "\"") + "}"));
	}

}
