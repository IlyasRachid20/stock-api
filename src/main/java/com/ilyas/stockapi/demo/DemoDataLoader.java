package com.ilyas.stockapi.demo;

import com.ilyas.stockapi.dto.AdjustmentRequest;
import com.ilyas.stockapi.dto.CategoryRequest;
import com.ilyas.stockapi.dto.CustomerRequest;
import com.ilyas.stockapi.dto.ProductRequest;
import com.ilyas.stockapi.dto.RestockRequest;
import com.ilyas.stockapi.dto.SaleItemResponse;
import com.ilyas.stockapi.dto.SaleRequest;
import com.ilyas.stockapi.dto.SaleResponse;
import com.ilyas.stockapi.dto.UserRequest;
import com.ilyas.stockapi.dto.OrderResponses.StatusRequest;
import com.ilyas.stockapi.entity.Category;
import com.ilyas.stockapi.entity.MovementType;
import com.ilyas.stockapi.entity.Role;
import com.ilyas.stockapi.entity.SaleStatus;
import com.ilyas.stockapi.repository.AppUserRepository;
import com.ilyas.stockapi.repository.CategoryRepository;
import com.ilyas.stockapi.repository.OrderStatusChangeRepository;
import com.ilyas.stockapi.repository.SaleItemRepository;
import com.ilyas.stockapi.repository.PriceChangeRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.repository.StockMovementRepository;
import com.ilyas.stockapi.service.CategoryService;
import com.ilyas.stockapi.service.CustomerService;
import com.ilyas.stockapi.service.OrderManagementService;
import com.ilyas.stockapi.service.ProductPictureService;
import com.ilyas.stockapi.service.ProductService;
import com.ilyas.stockapi.service.SaleService;
import com.ilyas.stockapi.service.UserService;
import com.ilyas.stockapi.shop.OrderDtos.OrderRequest;
import com.ilyas.stockapi.shop.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Fills an empty database with a small phone shop and 30 days of activity, so the public demo
 * shows real charts, best sellers, low-stock alerts and stock history right away.
 * Only active with app.demo.enabled=true (APP_DEMO_DATA). Everything goes through the normal
 * services (stock checks, history), then dates are moved back so the month looks lived-in.
 * A fixed random seed makes the generated shop the same on every start.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
@Order(2) // after AdminAccountInitializer
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final int DAYS = 30;

    // Who places the demo's online orders (as in the shop: anonymous) and who handles them
    private static final Authentication VISITOR = new AnonymousAuthenticationToken(
            "demo", "visitor", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    private static final Authentication STAFF = UsernamePasswordAuthenticationToken.authenticated(
            "admin", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));

    // name, phone, city, address
    private static final List<String[]> ONLINE_CUSTOMERS = List.of(
            new String[] {"Imane Alaoui", "0611223344", "Rabat", "14 avenue Mohammed V"},
            new String[] {"Hamza Bennani", "0622334455", "Casablanca", "27 rue Ibn Battouta, Maarif"},
            new String[] {"Nadia Chraibi", "0633445566", "Marrakech", "8 derb Lalla Azzouna"},
            new String[] {"Karim El Fassi", "0644556677", "Fes", "3 rue Talaa Kebira"},
            new String[] {"Yasmine Tahiri", "0655667788", "Tangier", "51 boulevard Pasteur"},
            new String[] {"Mehdi Sqalli", "0666778899", "Agadir", "19 avenue Hassan II"});

    // age: how many days ago it was placed (0 = today)
    private record DemoOrder(long saleId, Instant placedAt, int age) {
    }

    private final ProductRepository productRepository;
    private final AppUserRepository userRepository;
    private final SaleRepository saleRepository;
    private final StockMovementRepository movementRepository;
    private final CategoryRepository categoryRepository;
    private final PriceChangeRepository priceChangeRepository;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final ProductPictureService pictureService;
    private final OrderService orderService;
    private final OrderManagementService orderManagement;
    private final OrderStatusChangeRepository statusChangeRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerService customerService;
    private final SaleService saleService;
    private final UserService userService;
    private final String demoPassword;

    public DemoDataLoader(ProductRepository productRepository, AppUserRepository userRepository,
            SaleRepository saleRepository, StockMovementRepository movementRepository,
            CategoryRepository categoryRepository, PriceChangeRepository priceChangeRepository,
            CategoryService categoryService, ProductService productService, ProductPictureService pictureService,
            OrderService orderService, OrderManagementService orderManagement, OrderStatusChangeRepository statusChangeRepository,
            SaleItemRepository saleItemRepository, CustomerService customerService, SaleService saleService,
            UserService userService, @Value("${app.demo.password:}") String demoPassword) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
        this.movementRepository = movementRepository;
        this.categoryRepository = categoryRepository;
        this.priceChangeRepository = priceChangeRepository;
        this.categoryService = categoryService;
        this.productService = productService;
        this.pictureService = pictureService;
        this.orderService = orderService;
        this.orderManagement = orderManagement;
        this.statusChangeRepository = statusChangeRepository;
        this.saleItemRepository = saleItemRepository;
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

        Instant firstDay = LocalDate.now(ZoneOffset.UTC).minusDays(DAYS - 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Random random = new Random(42);

        long phones = category("Phones");
        long audio = category("Audio");
        long chargers = category("Chargers & cables");
        long protection = category("Protection");

        // name, price, initial stock, minimum level, category, description: phones first, then accessories.
        // Some prices change during the month (see below), so the shop shows price reductions.
        // Each product gets its picture from demo/pictures (drawn for the demo, see frontend/scripts).
        List<Long> products = List.of(
                product("Galaxy S26", "9999.00", 30, 5, phones,
                        "6.2-inch AMOLED screen, 256 GB of storage and a triple camera. Two-year warranty."),
                product("iPhone 17", "12900.00", 16, 5, phones,
                        "6.1-inch screen, 128 GB of storage and a battery that lasts all day. Two-year warranty."),
                product("Redmi Note 15", "2899.00", 45, 8, phones,
                        "Large 6.7-inch screen, 128 GB of storage and fast charging, at a friendly price."),
                product("AirPods Pro 3", "2590.00", 18, 6, audio,
                        "Wireless earbuds with noise cancellation and a USB-C charging case."),
                product("USB-C Charger 45W", "199.00", 90, 15, chargers,
                        "Fast wall charger for phones and tablets, with one USB-C port."),
                product("USB-C Cable 1m", "49.90", 60, 20, chargers,
                        "Braided USB-C to USB-C cable, one metre long, for charging and data."),
                product("Screen Protector", "79.00", 55, 15, protection,
                        "Tempered glass that protects the screen from scratches and small drops."),
                product("Phone Case", "149.00", 35, 10, protection,
                        "Slim shock-absorbing case with raised edges around the camera."));
        // The initial stock arrived the day before the first sale
        movementRepository.findAll().forEach(m -> {
            m.setCreatedAt(firstDay.minus(Duration.ofHours(14)));
            movementRepository.save(m);
        });

        List<Long> customers = List.of(
                customer("Ahmed Benali", "ahmed.benali@example.com", "0600000001"),
                customer("Sara El Idrissi", "sara.elidrissi@example.com", "0600000002"),
                customer("Youssef Amrani", null, "0600000003"),
                customer("Khadija Ouazzani", "khadija.ouazzani@example.com", null),
                customer("Omar Tazi", "omar.tazi@example.com", "0600000005"),
                customer("Salma Berrada", null, "0600000006"));

        int sales = 0;
        // Online orders get their own random numbers, so the counter sales stay the same
        Random orderRandom = new Random(7);
        List<DemoOrder> orders = new ArrayList<>();
        for (int day = 0; day < DAYS; day++) {
            Instant dayStart = firstDay.plus(Duration.ofDays(day));
            Instant morning = dayStart.plus(Duration.ofHours(8));
            // A price rise (no reduction shown), then two price drops shown struck through
            if (day == 5) {
                changePrice(products.get(3), "AirPods Pro 3", "2690.00", audio, morning);
            }
            if (day == 20) {
                changePrice(products.get(0), "Galaxy S26", "9500.00", phones, morning);
            }
            if (day == 25) {
                changePrice(products.get(7), "Phone Case", "129.00", protection, morning);
            }
            if (day == 12) {
                backdate(productService.restock(products.get(5), new RestockRequest(80, "Delivery #1042")).id(), morning);
            }
            if (day == 20) {
                backdate(productService.adjust(products.get(6), new AdjustmentRequest(-3, "Broken during delivery")).id(), morning);
            }
            // Opening hours 9:00-19:00, in time order so sale numbers follow the clock,
            // and never later than now (today's sales must not be in the future)
            List<Instant> times = new ArrayList<>();
            int salesToday = 1 + random.nextInt(3);
            for (int s = 0; s < salesToday; s++) {
                times.add(dayStart.plus(Duration.ofMinutes(9 * 60 + random.nextInt(10 * 60))));
            }
            Instant latest = Instant.now().minus(Duration.ofMinutes(5));
            for (Instant time : times.stream().sorted().toList()) {
                Instant when = time.isAfter(latest) ? latest : time;
                if (sale(customers.get(random.nextInt(customers.size())), products, random, when)) {
                    sales++;
                }
            }
            // An online order every three days in the evening, after the counter's sales, and two today
            int ordersToday = (day == DAYS - 1) ? 2 : (day % 3 == 1 ? 1 : 0);
            for (int o = 0; o < ordersToday; o++) {
                Instant time = dayStart.plus(Duration.ofMinutes(20 * 60 + o * 45 + orderRandom.nextInt(40)));
                Instant when = time.isAfter(latest) ? latest : time;
                int age = DAYS - 1 - day;
                onlineOrder(products, orderRandom, when).ifPresent(id -> orders.add(new DemoOrder(id, when, age)));
            }
        }
        handleOnlineOrders(orders);

        log.info("Demo mode: added {} products in 4 categories, {} customers, {} sales and {} online orders over {} days",
                products.size(), customers.size(), sales, orders.size(), DAYS);
    }

    // Reuses a category with that name if the shop already has one
    private long category(String name) {
        return categoryRepository.findByNameIgnoreCase(name).map(Category::getId)
                .orElseGet(() -> categoryService.create(new CategoryRequest(name)).id());
    }

    private long product(String name, String price, int quantity, int minQuantity, long categoryId, String description) {
        long id = productService.create(
                new ProductRequest(name, new BigDecimal(price), quantity, minQuantity, categoryId, description, true)).id();
        // "USB-C Cable 1m" -> demo/pictures/usb-c-cable-1m.jpg
        ClassPathResource picture = new ClassPathResource(
                "demo/pictures/" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-") + ".jpg");
        if (picture.exists()) {
            try (InputStream content = picture.getInputStream()) {
                pictureService.add(id, content, picture.contentLength());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return id;
    }

    private long customer(String name, String email, String phone) {
        return customerService.create(new CustomerRequest(name, email, phone)).id();
    }

    // One to three different products; phones sell one at a time, accessories up to three.
    // Lines the stock can't cover are left out, so the demo never fails on a sold-out product.
    private boolean sale(long customerId, List<Long> products, Random random, Instant when) {
        Set<Integer> picked = new LinkedHashSet<>();
        int lines = 1 + random.nextInt(3);
        while (picked.size() < lines) {
            picked.add(random.nextInt(products.size()));
        }
        List<SaleRequest.Item> items = new ArrayList<>();
        for (int index : picked) {
            int quantity = index < 4 ? 1 : 1 + random.nextInt(3);
            int inStock = productRepository.findById(products.get(index)).orElseThrow().getQuantity();
            if (inStock >= quantity) {
                items.add(new SaleRequest.Item(products.get(index), quantity, null));
            }
        }
        if (items.isEmpty()) {
            return false;
        }
        SaleResponse sale = saleService.createSale(new SaleRequest(customerId, items));

        saleRepository.findById(sale.id()).ifPresent(s -> {
            s.setSaleDate(when);
            saleRepository.save(s);
        });
        List<Long> itemIds = sale.items().stream().map(SaleItemResponse::id).toList();
        movementRepository.findBySaleItemIdIn(itemIds).forEach(m -> {
            m.setCreatedAt(when);
            movementRepository.save(m);
        });
        return true;
    }

    // Changes a price through the normal service (price history, struck-through price), then
    // moves the change back to that day of the demo month
    private void changePrice(long productId, String name, String price, long categoryId, Instant when) {
        String description = productRepository.findById(productId).orElseThrow().getDescription();
        productService.update(productId, new ProductRequest(name, new BigDecimal(price), null, null, categoryId, description, null));
        priceChangeRepository.findByProductIdOrderByChangedAtDescIdDesc(productId).stream().findFirst().ifPresent(change -> {
            change.setChangedAt(when);
            priceChangeRepository.save(change);
        });
        productRepository.findById(productId).filter(p -> p.getPriceReducedAt() != null).ifPresent(p -> {
            p.setPriceReducedAt(when);
            productRepository.save(p);
        });
    }

    // An online order placed as in the shop (anonymous visitor), then moved back to its moment.
    // Lines the stock can't cover are left out, so the demo never fails on a sold-out product.
    private java.util.Optional<Long> onlineOrder(List<Long> products, Random random, Instant when) {
        String[] who = ONLINE_CUSTOMERS.get(random.nextInt(ONLINE_CUSTOMERS.size()));
        // Mostly accessories, sometimes the Redmi phone
        List<Integer> choices = List.of(2, 3, 4, 5, 6, 7, 4, 5, 6, 7);
        Set<Integer> picked = new LinkedHashSet<>();
        int lines = 1 + random.nextInt(2);
        while (picked.size() < lines) {
            picked.add(choices.get(random.nextInt(choices.size())));
        }
        List<OrderRequest.Line> items = picked.stream()
                .map(products::get)
                .filter(id -> productRepository.findById(id).orElseThrow().getQuantity() > 0)
                .map(id -> new OrderRequest.Line(id, 1))
                .toList();
        if (items.isEmpty()) {
            return java.util.Optional.empty();
        }
        String number = as(VISITOR, () -> orderService.create(new OrderRequest(who[0], who[1], who[2], who[3], null, items))).orderNumber();

        var sale = saleRepository.findByOrderNumber(number).orElseThrow();
        sale.setSaleDate(when);
        saleRepository.save(sale);
        statusChangeRepository.findBySaleIdOrderByChangedAtAscIdAsc(sale.getId()).forEach(change -> {
            change.setChangedAt(when);
            statusChangeRepository.save(change);
        });
        List<Long> itemIds = saleItemRepository.findBySaleId(sale.getId()).stream().map(item -> item.getId()).toList();
        movementRepository.findBySaleItemIdIn(itemIds).forEach(m -> {
            m.setCreatedAt(when);
            movementRepository.save(m);
        });
        return java.util.Optional.of(sale.getId());
    }

    // The shop handled its orders: delivered when old enough, one returned at the door, one cancelled
    // by phone, recent ones confirmed or on their way, today's still to confirm
    private void handleOnlineOrders(List<DemoOrder> orders) {
        for (int index = 0; index < orders.size(); index++) {
            DemoOrder order = orders.get(index);
            int age = order.age();
            Instant placed = order.placedAt();
            if (age < 1) {
                continue;
            }
            step(order, SaleStatus.CONFIRMED, "Confirmed by phone", placed.plus(Duration.ofHours(14)));
            if (index == 3) {
                step(order, SaleStatus.CANCELLED, "Customer changed their mind", placed.plus(Duration.ofHours(15)));
                continue;
            }
            if (age < 3) {
                continue;
            }
            step(order, SaleStatus.SHIPPED, null, placed.plus(Duration.ofHours(20)));
            if (age < 6) {
                continue;
            }
            if (index == 1) {
                step(order, SaleStatus.RETURNED, "Refused at the door", placed.plus(Duration.ofHours(44)));
            } else {
                step(order, SaleStatus.DELIVERED, "Cash collected", placed.plus(Duration.ofHours(44)));
            }
        }
    }

    private void step(DemoOrder order, SaleStatus status, String note, Instant planned) {
        // Never in the future (yesterday evening's order is confirmed "this morning" at the latest now)
        Instant limit = Instant.now().minus(Duration.ofMinutes(1));
        Instant when = planned.isAfter(limit) ? limit : planned;
        as(STAFF, () -> orderManagement.changeStatus(order.saleId(), new StatusRequest(status, note)));
        var history = statusChangeRepository.findBySaleIdOrderByChangedAtAscIdAsc(order.saleId());
        var change = history.stream().max(java.util.Comparator.comparing(c -> c.getId())).orElseThrow();
        change.setChangedAt(when);
        statusChangeRepository.save(change);
        if (status.returnsStock()) {
            List<Long> itemIds = saleItemRepository.findBySaleId(order.saleId()).stream().map(item -> item.getId()).toList();
            movementRepository.findBySaleItemIdIn(itemIds).stream()
                    .filter(m -> m.getType() == MovementType.SALE_CANCELLED)
                    .forEach(m -> {
                        m.setCreatedAt(when);
                        movementRepository.save(m);
                    });
        }
    }

    // Runs the work as that user, as if they had made the request (for "created by" and the history)
    private static <T> T as(Authentication who, Supplier<T> work) {
        var context = SecurityContextHolder.getContext();
        Authentication before = context.getAuthentication();
        context.setAuthentication(who);
        try {
            return work.get();
        } finally {
            context.setAuthentication(before);
        }
    }

    // Moves the latest stock movement of a product (a restock or a correction just made) back in time
    private void backdate(long productId, Instant when) {
        movementRepository.findTopByProductIdOrderByIdDesc(productId).ifPresent(m -> {
            m.setCreatedAt(when);
            movementRepository.save(m);
        });
    }
}
