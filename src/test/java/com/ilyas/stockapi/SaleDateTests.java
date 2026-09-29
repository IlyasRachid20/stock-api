package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SaleDateTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void saleDateIsAnUtcInstant() throws Exception {
		String customer = mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ahmed\"}")).andReturn().getResponse().getContentAsString();
		Number customerId = JsonPath.read(customer, "$.id");

		String sale = mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
						.content("{\"customerId\":" + customerId + "}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String saleDate = JsonPath.read(sale, "$.saleDate");

		// e.g. "2026-09-29T19:27:31.575Z": the Z means UTC, so clients can convert it to local time
		assertThat(saleDate).endsWith("Z");
		assertThat(Duration.between(Instant.parse(saleDate), Instant.now()).abs()).isLessThan(Duration.ofMinutes(1));
	}

}
