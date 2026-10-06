package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.repository.CategoryRepository;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.repository.StockMovementRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
 * Guards against the "N+1 queries" problem: listing a page of sales must not run one extra
 * query per sale (or per item) to load customers, items and products.
 * Not @Transactional, so every request reads from the database like the running app.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class QueryCountTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private SaleItemRepository saleItemRepository;

	@Autowired
	private StockMovementRepository stockMovementRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private com.ilyas.stockapi.repository.PriceChangeRepository priceChangeRepository;

	@Autowired
	private com.ilyas.stockapi.repository.ProductImageRepository imageRepository;

	@Autowired
	private com.ilyas.stockapi.repository.ProductImageFileRepository imageFileRepository;

	@AfterEach
	void cleanUp() {
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
	void listingSalesUsesAFixedNumberOfQueries() throws Exception {
		for (int i = 1; i <= 10; i++) {
			long customer = idOf(postJson("/api/customers", "{\"name\":\"Customer " + i + "\"}"));
			long phone = idOf(postJson("/api/products", "{\"name\":\"Phone " + i + "\",\"price\":100.00,\"quantity\":5}"));
			long cable = idOf(postJson("/api/products", "{\"name\":\"Cable " + i + "\",\"price\":10.00,\"quantity\":5}"));
			long sale = idOf(postJson("/api/sales", "{\"customerId\":" + customer + "}"));
			postJson("/api/sale-items", "{\"saleId\":" + sale + ",\"productId\":" + phone + ",\"quantity\":1}");
			postJson("/api/sale-items", "{\"saleId\":" + sale + ",\"productId\":" + cable + ",\"quantity\":2}");
		}

		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		mockMvc.perform(get("/api/sales"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(10))
				.andExpect(jsonPath("$.content[0].items.length()").value(2))
				.andExpect(jsonPath("$.content[0].items[0].product.name").exists());

		// sales page + customers + items + products, each loaded in one batch.
		// Without batching this was 1 + 10 (items) + 10 (customers) + 20 (products).
		assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(5);
	}

	@Test
	void listingProductsLoadsTheirCategoriesInOneQuery() throws Exception {
		for (int i = 1; i <= 5; i++) {
			long category = idOf(postJson("/api/categories", "{\"name\":\"Category " + i + "\"}"));
			for (int j = 1; j <= 2; j++) {
				postJson("/api/products", "{\"name\":\"Product " + i + "-" + j + "\",\"price\":10.00,\"categoryId\":" + category + "}");
			}
		}

		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		mockMvc.perform(get("/api/products?size=20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(10))
				.andExpect(jsonPath("$.content[0].category.name").exists());

		// products page + their 5 categories in one batch + their pictures in one batch
		// (+ the total count when needed); picture bytes are never loaded for a list
		assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	private long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
