package com.ilyas.stockapi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.transaction.annotation.Transactional;

/** One connection can only place a few orders an hour (2 here), so a script can't hold the whole stock. */
@SpringBootTest(properties = "app.shop.max-orders-per-hour=2")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Transactional
class OrderLimitTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void aConnectionCanOnlyPlaceAFewOrdersAnHour() throws Exception {
		String json = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Limit Cable\",\"price\":49.90,\"quantity\":50}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long cable = ((Number) JsonPath.read(json, "$.id")).longValue();

		order(cable, "0611111111", "10.0.0.1").andExpect(status().isCreated());
		order(cable, "0622222222", "10.0.0.1").andExpect(status().isCreated());
		order(cable, "0633333333", "10.0.0.1")
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error").value("Too many orders from your connection: please try again later or contact us"));

		// Another connection is not affected
		order(cable, "0644444444", "10.0.0.2").andExpect(status().isCreated());
	}

	private ResultActions order(long productId, String phone, String address) throws Exception {
		return mockMvc.perform(post("/api/shop/orders").with(anonymous())
				.with(request -> {
					request.setRemoteAddr(address);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Visitor\",\"phone\":\"" + phone + "\",\"city\":\"Rabat\",\"address\":\"1 rue X\","
						+ "\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"));
	}

}
