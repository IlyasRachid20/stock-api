package com.ilyas.stockapi.shop;

import com.ilyas.stockapi.shop.OrderDtos.OrderRequest;
import com.ilyas.stockapi.shop.OrderDtos.OrderView;
import com.ilyas.stockapi.shop.OrderDtos.ShopInfo;
import com.ilyas.stockapi.shop.OrderDtos.TrackRequest;
import com.ilyas.stockapi.shop.ShopDtos.ShopCategory;
import com.ilyas.stockapi.shop.ShopDtos.ShopHome;
import com.ilyas.stockapi.shop.ShopDtos.ShopProduct;
import com.ilyas.stockapi.shop.ShopDtos.ShopProductDetail;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// The online shop: public, no login (see SecurityConfig). The catalog (published products only) and
// orders paid cash on delivery
@RestController
@RequestMapping("/api/shop")
@Tag(name = "Online shop", description = "Public catalog for the TechSouk website")
@SecurityRequirements()
public class ShopController {

    private final ShopService shopService;
    private final OrderService orderService;
    private final ShopProperties properties;

    public ShopController(ShopService shopService, OrderService orderService, ShopProperties properties) {
        this.shopService = shopService;
        this.orderService = orderService;
        this.properties = properties;
    }

    // Delivery fee and free delivery threshold, for the cart and the checkout
    @GetMapping("/info")
    public ShopInfo info() {
        return new ShopInfo("MAD", properties.deliveryFee(), properties.freeDeliveryFrom());
    }

    // Places an order paid cash on delivery: 201 with the order number to track it
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView placeOrder(@Valid @RequestBody OrderRequest request, HttpServletRequest http) {
        return orderService.place(request, http.getRemoteAddr());
    }

    // POST, not GET: the phone number stays out of addresses and server logs
    @PostMapping("/orders/track")
    public OrderView track(@Valid @RequestBody TrackRequest request) {
        return orderService.track(request);
    }

    @GetMapping("/home")
    public ShopHome home() {
        return shopService.home();
    }

    @GetMapping("/categories")
    public List<ShopCategory> categories() {
        return shopService.categories();
    }

    // GET /api/shop/products?category=phones&search=galaxy&sort=price,asc  or  ?ids=1,4 (the cart)
    @GetMapping("/products")
    public PagedModel<ShopProduct> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<Long> ids,
            @ParameterObject @PageableDefault(size = 12, sort = "name") Pageable pageable) {
        return new PagedModel<>(shopService.products(category, search, ids, pageable));
    }

    @GetMapping("/products/{id}")
    public ShopProductDetail product(@PathVariable Long id) {
        return shopService.product(id);
    }
}
