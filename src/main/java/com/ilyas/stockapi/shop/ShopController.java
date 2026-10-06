package com.ilyas.stockapi.shop;

import com.ilyas.stockapi.shop.ShopDtos.ShopCategory;
import com.ilyas.stockapi.shop.ShopDtos.ShopHome;
import com.ilyas.stockapi.shop.ShopDtos.ShopProduct;
import com.ilyas.stockapi.shop.ShopDtos.ShopProductDetail;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// The online shop's catalog: public, no login (see SecurityConfig), published products only
@RestController
@RequestMapping("/api/shop")
@Tag(name = "Online shop", description = "Public catalog for the TechSouk website")
@SecurityRequirements()
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
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
