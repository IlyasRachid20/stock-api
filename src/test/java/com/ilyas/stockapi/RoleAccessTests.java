package com.ilyas.stockapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.entity.Customer;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a CASHIER may and may not do. An ADMIN may do everything (the other test classes run as ADMIN).
 *
 *   Resource     | GET  | POST | PUT  | DELETE
 *   -------------+------+------+------+-------
 *   products     | yes  | no   | no   | no       prices and stock are the admin's job
 *   customers    | yes  | yes  | yes  | no       a cashier registers customers at the till
 *   sales        | yes  | yes  |  -   | no       a cashier can't erase a sale
 *   sale-items   | yes  | yes  |  -   | no
 *
 * "no" means 403 Forbidden, checked before the request reaches the controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "CASHIER")
@Transactional
class RoleAccessTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private SaleRepository saleRepository;

	private long customerId;
	private long productId;
	private long saleId;

	// Test data is created directly in the database, since a cashier isn't allowed to create products
	@BeforeEach
	void createData() {
		Customer customer = new Customer();
		customer.setName("Ahmed");
		customerId = customerRepository.save(customer).getId();

		Product product = new Product();
		product.setName("Galaxy S26");
		product.setPrice(new BigDecimal("9500.00"));
		product.setQuantity(10);
		productId = productRepository.save(product).getId();

		Sale sale = new Sale();
		sale.setCustomer(customer);
		saleId = saleRepository.save(sale).getId();
	}

	// ----- Allowed for a cashier -----

	@Test
	void cashierCanReadEverything() throws Exception {
		mockMvc.perform(get("/api/products")).andExpect(status().isOk());
		mockMvc.perform(get("/api/products/" + productId)).andExpect(status().isOk());
		mockMvc.perform(get("/api/customers")).andExpect(status().isOk());
		mockMvc.perform(get("/api/sales")).andExpect(status().isOk());
		mockMvc.perform(get("/api/sale-items")).andExpect(status().isOk());
	}

	@Test
	void cashierCanRegisterAndUpdateCustomers() throws Exception {
		mockMvc.perform(json(post("/api/customers"), "{\"name\":\"Sara\"}")).andExpect(status().isCreated());
		mockMvc.perform(json(put("/api/customers/" + customerId), "{\"name\":\"Ahmed R.\"}")).andExpect(status().isOk());
	}

	@Test
	void cashierCanSell() throws Exception {
		mockMvc.perform(json(post("/api/sales"), "{\"customerId\":" + customerId + "}")).andExpect(status().isCreated());
		mockMvc.perform(json(post("/api/sale-items"), "{\"saleId\":" + saleId + ",\"productId\":" + productId + ",\"quantity\":1}"))
				.andExpect(status().isCreated());
	}

	// ----- Forbidden for a cashier -----

	@Test
	void cashierCannotCreateProducts() throws Exception {
		mockMvc.perform(json(post("/api/products"), "{\"name\":\"Cable\",\"price\":49.90}")).andExpect(status().isForbidden());
	}

	@Test
	void cashierCannotChangeProductsPriceOrStock() throws Exception {
		mockMvc.perform(json(put("/api/products/" + productId), "{\"name\":\"Galaxy S26\",\"price\":1.00,\"quantity\":999}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void cashierCannotDeleteProducts() throws Exception {
		mockMvc.perform(delete("/api/products/" + productId)).andExpect(status().isForbidden());
	}

	@Test
	void cashierCannotDeleteCustomers() throws Exception {
		mockMvc.perform(delete("/api/customers/" + customerId)).andExpect(status().isForbidden());
	}

	@Test
	void cashierCannotDeleteSales() throws Exception {
		mockMvc.perform(delete("/api/sales/" + saleId)).andExpect(status().isForbidden());
	}

	@Test
	void cashierCannotDeleteSaleItems() throws Exception {
		// The id doesn't need to exist: the role check happens before the controller runs
		mockMvc.perform(delete("/api/sale-items/999999")).andExpect(status().isForbidden());
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

}
