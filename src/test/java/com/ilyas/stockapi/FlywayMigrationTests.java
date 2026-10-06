package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The app starting at all proves Hibernate's "validate" accepted the tables Flyway created.
 * These tests also check which migrations ran and that the V2 indexes exist.
 */
@SpringBootTest
class FlywayMigrationTests {

	@Autowired
	private Flyway flyway;

	@Autowired
	private DataSource dataSource;

	@Test
	void allMigrationsAreApplied() {
		MigrationInfo[] applied = flyway.info().applied();

		assertThat(Arrays.stream(applied).map(info -> info.getVersion().getVersion()))
				.containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
		assertThat(flyway.info().pending()).isEmpty();
	}

	@Test
	void foreignKeyIndexesExist() throws Exception {
		assertThat(indexNames("sales")).contains("idx_sales_customer_id");
		assertThat(indexNames("sale_items")).contains("idx_sale_items_sale_id", "idx_sale_items_product_id");
		assertThat(indexNames("products")).contains("idx_products_category_id");
		assertThat(indexNames("price_changes")).contains("idx_price_changes_product_id");
		assertThat(indexNames("product_images")).contains("idx_product_images_product_id");
	}

	// Standard JDBC metadata, so this works on both H2 (upper-case names) and PostgreSQL (lower-case)
	private List<String> indexNames(String table) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			String name = metaData.storesUpperCaseIdentifiers() ? table.toUpperCase() : table;
			List<String> names = new ArrayList<>();
			try (ResultSet rs = metaData.getIndexInfo(null, null, name, false, false)) {
				while (rs.next()) {
					if (rs.getString("INDEX_NAME") != null) {
						names.add(rs.getString("INDEX_NAME").toLowerCase());
					}
				}
			}
			return names;
		}
	}

}
