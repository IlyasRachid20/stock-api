package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilyas.stockapi.shop.Slugs;
import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The online shop's public catalog. Test data is created through the staff API (as an admin);
 * the shop is then read as an anonymous visitor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Transactional
class ShopTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void catalogIsOpenToEveryoneEvenWithABrokenToken() throws Exception {
		mockMvc.perform(get("/api/shop/products").with(anonymous())).andExpect(status().isOk());
		mockMvc.perform(get("/api/shop/home").with(anonymous())).andExpect(status().isOk());
		// A stale token left in a visitor's browser must not break the shop
		mockMvc.perform(get("/api/shop/categories").with(anonymous()).header("Authorization", "Bearer broken"))
				.andExpect(status().isOk());
	}

	@Test
	void hiddenProductsAreNotInTheShopButStillForSale() throws Exception {
		long hidden = product("Gift Card Shoptest", 100, null, false);

		shop("/api/shop/products?search=shoptest").andExpect(jsonPath("$.page.totalElements").value(0));
		shop("/api/shop/products/" + hidden).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/products/" + hidden)).andExpect(jsonPath("$.published").value(false));

		// Leaving "published" out of an update keeps it as it is
		send(put("/api/products/" + hidden), "{\"name\":\"Gift Card Shoptest\",\"price\":50.00}")
				.andExpect(jsonPath("$.published").value(false));
	}

	@Test
	void shopShowsAvailabilityNotTheExactStock() throws Exception {
		product("Plenty Shoptest", 50, null, true);
		product("Few Shoptest", 3, null, true);
		product("None Shoptest", 0, null, true);

		String json = shop("/api/shop/products?search=shoptest&sort=name")
				.andExpect(jsonPath("$.content[*].name", contains("Few Shoptest", "None Shoptest", "Plenty Shoptest")))
				.andExpect(jsonPath("$.content[*].availability", contains("FEW_LEFT", "OUT_OF_STOCK", "IN_STOCK")))
				.andExpect(jsonPath("$.content[0].onlyLeft").value(3))
				.andExpect(jsonPath("$.content[2].onlyLeft").value(nullValue()))
				.andExpect(jsonPath("$.content[*].maxQuantity", contains(3, 0, 10)))
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> card = JsonPath.read(json, "$.content[0]");
		assertThat(card.keySet()).containsExactlyInAnyOrder("id", "slug", "name", "category", "price", "previousPrice",
				"availability", "onlyLeft", "maxQuantity", "imageUrl");
	}

	@Test
	void productsAreFilteredByCategorySlugOrIds() throws Exception {
		long chargers = category("Chargers & cables");
		long charger = product("Charger Shoptest", 10, chargers, true);
		long cable = product("Cable Shoptest", 10, chargers, true);
		long phone = product("Phone Shoptest", 10, null, true);

		shop("/api/shop/products?category=chargers-cables")
				.andExpect(jsonPath("$.content[*].id", containsInAnyOrder((int) charger, (int) cable)))
				.andExpect(jsonPath("$.content[0].category.slug").value("chargers-cables"));
		shop("/api/shop/products?category=nothing-here")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("No category 'nothing-here' in the shop"));
		shop("/api/shop/products?ids=" + phone + "," + cable)
				.andExpect(jsonPath("$.content[*].id", containsInAnyOrder((int) phone, (int) cable)));
	}

	@Test
	void productPageHasItsDescriptionPicturesAndReadableName() throws Exception {
		long id = idOf(send(post("/api/products"),
				"{\"name\":\"Écouteurs Pro\",\"price\":499.00,\"quantity\":8,\"description\":\"Wireless earbuds\"}"));
		mockMvc.perform(multipart("/api/products/" + id + "/images").file(new MockMultipartFile("file", png())))
				.andExpect(status().isCreated());

		shop("/api/shop/products/" + id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value("ecouteurs-pro"))
				.andExpect(jsonPath("$.description").value("Wireless earbuds"))
				.andExpect(jsonPath("$.images[0].url", startsWith("/api/images/")))
				.andExpect(jsonPath("$.quantity").doesNotExist());
	}

	@Test
	void homeHasCategoriesDealsAndBestSellers() throws Exception {
		long category = category("Hometest Gadgets");
		long deal = product("Deal Hometest", 20, category, true);
		send(put("/api/products/" + deal), "{\"name\":\"Deal Hometest\",\"price\":80.00,\"categoryId\":" + category + "}")
				.andExpect(jsonPath("$.previousPrice").value(100.00));
		long top = product("Top Hometest", 20, category, true);
		long second = product("Second Hometest", 20, category, true);
		long hidden = product("Hidden Hometest", 20, category, false);
		sell(top, 5);
		sell(second, 2);
		sell(hidden, 9);

		shop("/api/shop/home")
				.andExpect(jsonPath("$.deals[*].name", hasItem("Deal Hometest")))
				.andExpect(jsonPath("$.bestSellers[*].name", containsInRelativeOrder("Top Hometest", "Second Hometest")))
				.andExpect(jsonPath("$.bestSellers[*].name", not(hasItem("Hidden Hometest"))))
				// The hidden product isn't counted in its category
				.andExpect(jsonPath("$.categories[?(@.slug == 'hometest-gadgets')].productCount", contains(3)));
	}

	@Test
	void slugsAreReadableNames() {
		assertThat(Slugs.of("Chargers & cables")).isEqualTo("chargers-cables");
		assertThat(Slugs.of("  USB-C Cable 1m ")).isEqualTo("usb-c-cable-1m");
		assertThat(Slugs.of("Écouteurs sans fil")).isEqualTo("ecouteurs-sans-fil");
	}

	private ResultActions shop(String path) throws Exception {
		return mockMvc.perform(get(path).with(anonymous()));
	}

	private long category(String name) throws Exception {
		return idOf(send(post("/api/categories"), "{\"name\":\"" + name + "\"}"));
	}

	private long product(String name, int quantity, Long categoryId, boolean published) throws Exception {
		return idOf(send(post("/api/products"), "{\"name\":\"" + name + "\",\"price\":100.00,\"quantity\":" + quantity
				+ ",\"categoryId\":" + categoryId + ",\"published\":" + published + "}"));
	}

	private void sell(long productId, int quantity) throws Exception {
		long customer = idOf(send(post("/api/customers"), "{\"name\":\"Hometest customer\"}"));
		send(post("/api/sales"), "{\"customerId\":" + customer + ",\"items\":[{\"productId\":" + productId
				+ ",\"quantity\":" + quantity + "}]}").andExpect(status().isCreated());
	}

	private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
		return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

	private static byte[] png() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", bytes);
		return bytes.toByteArray();
	}

}
