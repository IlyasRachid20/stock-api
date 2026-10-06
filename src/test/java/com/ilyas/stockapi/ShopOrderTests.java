package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleStatus;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.shop.Phones;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
 * Orders placed in the online shop by anonymous visitors, paid cash on delivery. Products are set
 * up through the staff API (as an admin); orders are then placed without any login.
 * (The limit of orders per hour has its own test class: OrderLimitTests.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Transactional
class ShopOrderTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private EntityManager entityManager;

	private long phone;
	private long cable;

	@BeforeEach
	void createProducts() throws Exception {
		phone = product("Galaxy S26 Order", "9500.00", 10, true);
		cable = product("USB-C Cable Order", "49.90", 50, true);
	}

	@Test
	void orderIsPlacedAtDatabasePricesAndTakesTheStockAtOnce() throws Exception {
		// The cable is in two lines: they become one line of 3
		order(" Sara El Idrissi ", "06 12 34 56 78", line(phone, 1) + "," + line(cable, 2) + "," + line(cable, 1))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderNumber", matchesPattern("TS-[A-HJ-NP-Z2-9]{6}")))
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.items[*].name", contains("Galaxy S26 Order", "USB-C Cable Order")))
				.andExpect(jsonPath("$.items[1].quantity").value(3))
				.andExpect(jsonPath("$.subtotal").value(9649.70))
				// Free delivery from MAD 500
				.andExpect(jsonPath("$.deliveryFee").value(0.00))
				.andExpect(jsonPath("$.total").value(9649.70))
				.andExpect(jsonPath("$.delivery.name").value("Sara El Idrissi"))
				.andExpect(jsonPath("$.delivery.phone").value("+212612345678"))
				.andExpect(jsonPath("$.history[*].status", contains("NEW")));

		mockMvc.perform(get("/api/products/" + phone)).andExpect(jsonPath("$.quantity").value(9));
		mockMvc.perform(get("/api/products/" + cable)).andExpect(jsonPath("$.quantity").value(47));
		mockMvc.perform(get("/api/stock-movements?productId=" + phone))
				.andExpect(jsonPath("$.content[0].type").value("SALE"))
				.andExpect(jsonPath("$.content[0].createdBy").value("online shop"));
	}

	@Test
	void smallOrdersPayTheDeliveryFee() throws Exception {
		order("Omar", "0611111111", line(cable, 2))
				.andExpect(jsonPath("$.subtotal").value(99.80))
				.andExpect(jsonPath("$.deliveryFee").value(30.00))
				.andExpect(jsonPath("$.total").value(129.80));
	}

	@Test
	void anOrderIsOnlyRevenueOnceDelivered() throws Exception {
		mockMvc.perform(get("/api/reports/summary")).andExpect(jsonPath("$.salesCount").value(0));
		String number = numberOf(order("Omar", "0611111111", line(phone, 1)));

		// Cash on delivery: nothing is received yet
		mockMvc.perform(get("/api/reports/summary")).andExpect(jsonPath("$.salesCount").value(0));

		Sale sale = saleRepository.findByOrderNumber(number).orElseThrow();
		sale.setStatus(SaleStatus.DELIVERED);
		entityManager.flush();
		mockMvc.perform(get("/api/reports/summary"))
				.andExpect(jsonPath("$.salesCount").value(1))
				.andExpect(jsonPath("$.revenue").value(9500.00));
	}

	@Test
	void returningCustomerIsFoundByPhoneWhateverItsFormat() throws Exception {
		String first = numberOf(order("Sara", "0612345678", line(cable, 1)));
		String second = numberOf(order("Sara E.", "+212 6 12 34 56 78", line(cable, 1)));

		Sale a = saleRepository.findByOrderNumber(first).orElseThrow();
		Sale b = saleRepository.findByOrderNumber(second).orElseThrow();
		assertThat(a.getCustomer().getId()).isEqualTo(b.getCustomer().getId());
		assertThat(customerRepository.findFirstByPhoneOrderByIdAsc("+212612345678")).isPresent();
	}

	@Test
	void productHiddenFromTheShopCannotBeOrdered() throws Exception {
		long hidden = product("Hidden Order Item", "10.00", 5, false);

		order("Sara", "0612345678", line(hidden, 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Hidden Order Item is not sold online any more: remove it from your cart"));
	}

	@Test
	void shortStockIsRefusedWithoutShowingTheExactStock() throws Exception {
		long few = product("Few Order Item", "10.00", 1, true);

		order("Sara", "0612345678", line(few, 2))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Not enough stock for Few Order Item: lower the quantity or remove it from your cart"));
	}

	@Test
	void invalidOrdersAreExplainedFieldByField() throws Exception {
		send("{\"name\":\" \",\"phone\":\"12\",\"city\":\"Rabat\",\"address\":\"1 rue X\",\"items\":[]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.phone").value("must be a phone number, e.g. 06 12 34 56 78"))
				.andExpect(jsonPath("$.errors.items").exists());

		order("Sara", "0612345678", line(cable, 11))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors['items[0].quantity']").exists());
	}

	@Test
	void aPhoneWithThreeOrdersWaitingMustWaitForTheCall() throws Exception {
		for (int i = 0; i < 3; i++) {
			order("Sara", "0612345678", line(cable, 1)).andExpect(status().isCreated());
		}

		order("Sara", "06 12 34 56 78", line(cable, 1))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error").value("You already have 3 orders waiting for our call: we'll contact you soon to confirm them"));
	}

	@Test
	void orderIsTrackedWithItsNumberAndThePhoneUsed() throws Exception {
		String number = numberOf(order("Sara", "0612345678", line(cable, 1)));

		track(number.toLowerCase(), "+212 612 345 678")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderNumber").value(number))
				.andExpect(jsonPath("$.status").value("NEW"));
		track(number, "0699999999")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("No order " + number + " with this phone number"));
	}

	@Test
	void anOnlineOrderCantBeDeletedLikeACounterSale() throws Exception {
		String number = numberOf(order("Sara", "0612345678", line(cable, 1)));
		long id = saleRepository.findByOrderNumber(number).orElseThrow().getId();

		mockMvc.perform(delete("/api/sales/" + id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Online order " + number + " can't be deleted: cancel it instead"));
	}

	@Test
	void shopInfoGivesTheDeliveryRules() throws Exception {
		mockMvc.perform(get("/api/shop/info").with(anonymous()))
				.andExpect(jsonPath("$.currency").value("MAD"))
				.andExpect(jsonPath("$.deliveryFee").value(30.00))
				.andExpect(jsonPath("$.freeDeliveryFrom").value(500.00));
	}

	@Test
	void phoneNumbersAreWrittenOneWay() {
		assertThat(Phones.normalize("06 12 34 56 78")).isEqualTo("+212612345678");
		assertThat(Phones.normalize("00212 6-12-34-56-78")).isEqualTo("+212612345678");
		assertThat(Phones.normalize("+33 (6) 12 34 56 78")).isEqualTo("+33612345678");
	}

	private ResultActions order(String name, String phoneNumber, String lines) throws Exception {
		return send("{\"name\":\"" + name + "\",\"phone\":\"" + phoneNumber + "\",\"city\":\"Casablanca\","
				+ "\"address\":\"12 rue des Fleurs\",\"items\":[" + lines + "]}");
	}

	private ResultActions send(String body) throws Exception {
		return mockMvc.perform(post("/api/shop/orders").with(anonymous()).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions track(String number, String phoneNumber) throws Exception {
		return mockMvc.perform(post("/api/shop/orders/track").with(anonymous()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"orderNumber\":\"" + number + "\",\"phone\":\"" + phoneNumber + "\"}"));
	}

	private static String line(long productId, int quantity) {
		return "{\"productId\":" + productId + ",\"quantity\":" + quantity + "}";
	}

	private static String numberOf(ResultActions result) throws Exception {
		return JsonPath.read(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.orderNumber");
	}

	private long product(String name, String price, int quantity, boolean published) throws Exception {
		String json = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\",\"price\":" + price + ",\"quantity\":" + quantity + ",\"published\":" + published + "}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		Number id = JsonPath.read(json, "$.id");
		return id.longValue();
	}

}
