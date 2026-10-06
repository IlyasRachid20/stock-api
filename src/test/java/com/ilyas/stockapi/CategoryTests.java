package com.ilyas.stockapi;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product categories: unique names, products filed in a category, filtering by category,
 * and a category can only be deleted once it's empty. (What a cashier may do: RoleAccessTests.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Transactional
class CategoryTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void categoriesAreListedAlphabeticallyWithTheirNumberOfProducts() throws Exception {
		long phones = idOf(send(post("/api/categories"), "{\"name\":\"  Phones \"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Phones"))
				.andExpect(jsonPath("$.productCount").value(0)));
		send(post("/api/categories"), "{\"name\":\"Audio\"}").andExpect(status().isCreated());
		createProduct("Galaxy S26", phones);
		createProduct("iPhone 17", phones);

		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", containsInRelativeOrder("Audio", "Phones")))
				.andExpect(jsonPath("$[?(@.name == 'Phones')].productCount", contains(2)))
				.andExpect(jsonPath("$[?(@.name == 'Audio')].productCount", contains(0)));
	}

	@Test
	void categoryNamesAreUniqueIgnoringCase() throws Exception {
		long phones = createCategory("Phones");
		long audio = createCategory("Audio");

		send(post("/api/categories"), "{\"name\":\"phones\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("A category named 'phones' already exists"));
		send(put("/api/categories/" + audio), "{\"name\":\"PHONES\"}")
				.andExpect(status().isConflict());

		// Changing only the case of its own name is fine
		send(put("/api/categories/" + phones), "{\"name\":\"phones\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("phones"));
	}

	@Test
	void categoryNameIsRequired() throws Exception {
		send(post("/api/categories"), "{\"name\":\"   \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void productIsFiledInACategoryAndCanBeMovedOrTakenOut() throws Exception {
		long phones = createCategory("Phones");
		long audio = createCategory("Audio");

		long product = idOf(send(post("/api/products"), productJson("AirPods Pro 3", phones))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.category.id").value(phones))
				.andExpect(jsonPath("$.category.name").value("Phones")));

		send(put("/api/products/" + product), productJson("AirPods Pro 3", audio))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category.name").value("Audio"));

		// Without categoryId the product has no category any more
		send(put("/api/products/" + product), "{\"name\":\"AirPods Pro 3\",\"price\":2690.00}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category").isEmpty());
	}

	@Test
	void productInAnUnknownCategoryIsRejected() throws Exception {
		send(post("/api/products"), productJson("Galaxy S26", 999999L))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Category 999999 does not exist"));
	}

	@Test
	void productsCanBeFilteredByCategoryAndName() throws Exception {
		long phones = createCategory("Phones");
		long chargers = createCategory("Chargers & cables");
		createProduct("Galaxy S26 Filter", phones);
		createProduct("Redmi Note 15 Filter", phones);
		createProduct("USB-C Charger Filter", chargers);
		createProduct("Gift Card Filter", null);

		mockMvc.perform(get("/api/products?categoryId=" + phones + "&sort=name"))
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].name", contains("Galaxy S26 Filter", "Redmi Note 15 Filter")));
		mockMvc.perform(get("/api/products?categoryId=" + phones + "&search=redmi"))
				.andExpect(jsonPath("$.content[*].name", contains("Redmi Note 15 Filter")));
		// Without a category filter, products without a category are listed too
		mockMvc.perform(get("/api/products?search=filter"))
				.andExpect(jsonPath("$.page.totalElements").value(4))
				.andExpect(jsonPath("$.content[*].name", hasItem("Gift Card Filter")));
	}

	@Test
	void sortingByCategoryKeepsProductsWithoutOne() throws Exception {
		long phones = createCategory("Phones");
		long audio = createCategory("Audio");
		createProduct("Galaxy S26 Sorted", phones);
		createProduct("AirPods Sorted", audio);
		createProduct("Gift Card Sorted", null);

		mockMvc.perform(get("/api/products?search=sorted&sort=category.name"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(3))
				.andExpect(jsonPath("$.content[*].name", containsInRelativeOrder("AirPods Sorted", "Galaxy S26 Sorted")))
				.andExpect(jsonPath("$.content[*].name", hasItem("Gift Card Sorted")));
	}

	@Test
	void categoryCanOnlyBeDeletedOnceEmpty() throws Exception {
		long phones = createCategory("Phones");
		long product = createProduct("Galaxy S26", phones);

		mockMvc.perform(delete("/api/categories/" + phones))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Category 'Phones' cannot be deleted: it has 1 product(s)"));

		send(put("/api/products/" + product), "{\"name\":\"Galaxy S26\",\"price\":9500.00}").andExpect(status().isOk());
		mockMvc.perform(delete("/api/categories/" + phones)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/categories")).andExpect(jsonPath("$[*].name", not(hasItem("Phones"))));
	}

	@Test
	void unknownCategoryIsNotFound() throws Exception {
		send(put("/api/categories/999999"), "{\"name\":\"Audio\"}").andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/categories/999999")).andExpect(status().isNotFound());
	}

	private long createCategory(String name) throws Exception {
		return idOf(send(post("/api/categories"), "{\"name\":\"" + name + "\"}").andExpect(status().isCreated()));
	}

	private long createProduct(String name, Long categoryId) throws Exception {
		return idOf(send(post("/api/products"), productJson(name, categoryId)).andExpect(status().isCreated()));
	}

	private static String productJson(String name, Long categoryId) {
		return "{\"name\":\"" + name + "\",\"price\":100.00,\"quantity\":5,\"categoryId\":" + categoryId + "}";
	}

	private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
		return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

}
