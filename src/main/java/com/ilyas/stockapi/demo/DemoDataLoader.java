package com.ilyas.stockapi.demo;

import com.ilyas.stockapi.dto.CustomerRequest;
import com.ilyas.stockapi.dto.ProductRequest;
import com.ilyas.stockapi.dto.SaleItemRequest;
import com.ilyas.stockapi.dto.SaleRequest;
import com.ilyas.stockapi.dto.UserRequest;
import com.ilyas.stockapi.entity.Role;
import com.ilyas.stockapi.repository.AppUserRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.service.CustomerService;
import com.ilyas.stockapi.service.ProductService;
import com.ilyas.stockapi.service.SaleService;
import com.ilyas.stockapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Fills an empty database with a small shop, so the public demo shows real data right away.
 * Only active with app.demo.enabled=true (APP_DEMO_DATA). Goes through the normal services,
 * so sales take stock out and everything appears in the stock history.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
@Order(2) // after AdminAccountInitializer
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SaleService saleService;
    private final UserService userService;
    private final String demoPassword;

    public DemoDataLoader(ProductRepository productRepository, AppUserRepository userRepository,
            ProductService productService, CustomerService customerService, SaleService saleService,
            UserService userService, @Value("${app.demo.password:}") String demoPassword) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productService = productService;
        this.customerService = customerService;
        this.saleService = saleService;
        this.userService = userService;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (demoPassword != null && !demoPassword.isBlank() && !userRepository.existsByUsername("demo")) {
            userService.create(new UserRequest("demo", demoPassword, Role.CASHIER));
            log.info("Demo mode: created the 'demo' cashier account");
        }
        if (productRepository.count() > 0) {
            return;
        }

        // name, price, stock, minimum level: some products end up on the low-stock list
        List<Long> products = new ArrayList<>();
        products.add(product("Galaxy S26", "9500.00", 12, 3));
        products.add(product("iPhone 17", "12900.00", 8, 3));
        products.add(product("Redmi Note 15", "2899.00", 25, 5));
        products.add(product("AirPods Pro 3", "2690.00", 4, 5));
        products.add(product("USB-C Charger 45W", "199.00", 40, 10));
        products.add(product("USB-C Cable 1m", "49.90", 60, 15));
        products.add(product("Screen Protector", "79.00", 6, 10));
        products.add(product("Phone Case", "129.00", 0, 5));

        long ahmed = customer("Ahmed Benali", "ahmed.benali@example.com", "0600000001");
        long sara = customer("Sara El Idrissi", "sara.elidrissi@example.com", "0600000002");
        long youssef = customer("Youssef Amrani", null, "0600000003");

        sale(ahmed, products.get(0), 1, products.get(4), 1);
        sale(sara, products.get(1), 1, products.get(3), 1);
        sale(youssef, products.get(2), 2, products.get(5), 3);
        sale(ahmed, products.get(6), 2, products.get(5), 1);

        log.info("Demo mode: added {} products, 3 customers and 4 sales", products.size());
    }

    private long product(String name, String price, int quantity, int minQuantity) {
        return productService.create(new ProductRequest(name, new BigDecimal(price), quantity, minQuantity)).id();
    }

    private long customer(String name, String email, String phone) {
        return customerService.create(new CustomerRequest(name, email, phone)).id();
    }

    private void sale(long customerId, long product1, int quantity1, long product2, int quantity2) {
        long saleId = saleService.createSale(new SaleRequest(customerId)).id();
        saleService.addItem(new SaleItemRequest(saleId, product1, quantity1, null));
        saleService.addItem(new SaleItemRequest(saleId, product2, quantity2, null));
    }
}
