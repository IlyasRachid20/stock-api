package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductRepositoryTests {

	@Autowired
	private ProductRepository productRepository;

	@Test
	void savedProductCanBeFoundById() {
		Product product = new Product();
		product.setName("Galaxy S26");
		product.setPrice(new BigDecimal("9500.00"));
		product.setQuantity(10);

		Product saved = productRepository.save(product);

		assertThat(saved.getId()).isNotNull();
		assertThat(productRepository.findById(saved.getId()))
				.hasValueSatisfying(found -> {
					assertThat(found.getName()).isEqualTo("Galaxy S26");
					assertThat(found.getPrice()).isEqualByComparingTo("9500.00");
					assertThat(found.getQuantity()).isEqualTo(10);
				});
	}

	@Test
	void newProductDefaultsToZeroQuantity() {
		Product product = new Product();
		product.setName("Cable");
		product.setPrice(new BigDecimal("49.90"));

		Product saved = productRepository.save(product);

		assertThat(saved.getQuantity()).isZero();
	}

}
