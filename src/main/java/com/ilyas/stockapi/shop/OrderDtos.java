package com.ilyas.stockapi.shop;

import com.ilyas.stockapi.entity.OrderStatusChange;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleItem;
import com.ilyas.stockapi.entity.SaleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** What the online shop's checkout and order tracking send and receive. */
public final class OrderDtos {

    private OrderDtos() {
    }

    private static String clean(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    /**
     * Body of POST /api/shop/orders. Prices are not sent: they always come from the database.
     * The phone is normalized before validation (see Phones).
     */
    public record OrderRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "\\+?\\d{9,15}", message = "must be a phone number, e.g. 06 12 34 56 78") String phone,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 255) String address,
            @Size(max = 500) String note,
            @NotEmpty @Size(max = 20) List<@Valid Line> items) {

        public OrderRequest {
            name = clean(name);
            phone = Phones.normalize(clean(phone));
            city = clean(city);
            address = clean(address);
            note = clean(note);
        }

        public record Line(@NotNull Long productId, @NotNull @Min(1) @Max(ShopDtos.MAX_PER_ORDER) Integer quantity) {
        }
    }

    /** Body of POST /api/shop/orders/track: the number and the phone used, so strangers can't read an order. */
    public record TrackRequest(@NotBlank String orderNumber, @NotBlank String phone) {

        public TrackRequest {
            orderNumber = (orderNumber == null) ? null : orderNumber.trim().toUpperCase(java.util.Locale.ROOT);
            phone = Phones.normalize(clean(phone));
        }
    }

    public record OrderLine(Long productId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

        static OrderLine from(SaleItem item) {
            return new OrderLine(item.getProduct().getId(), item.getProduct().getName(), item.getQuantity(),
                    item.getUnitPrice(), item.getLineTotal());
        }
    }

    public record DeliveryInfo(String name, String phone, String city, String address, String note) {
    }

    /** A step of the order's story, without who did it (that's for the staff). */
    public record StatusStep(SaleStatus status, Instant at) {

        static StatusStep from(OrderStatusChange change) {
            return new StatusStep(change.getStatus(), change.getChangedAt());
        }
    }

    /** An order as its customer sees it: after placing it, and when tracking it. */
    public record OrderView(String orderNumber, SaleStatus status, Instant placedAt, List<OrderLine> items,
                            BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal total, DeliveryInfo delivery,
                            List<StatusStep> history) {

        static OrderView from(Sale sale, List<OrderStatusChange> history) {
            BigDecimal subtotal = sale.getTotal();
            return new OrderView(sale.getOrderNumber(), sale.getStatus(), sale.getSaleDate(),
                    sale.getItems().stream().map(OrderLine::from).toList(),
                    subtotal, sale.getDeliveryFee(), subtotal.add(sale.getDeliveryFee()),
                    new DeliveryInfo(sale.getDelivery().getName(), sale.getDelivery().getPhone(), sale.getDelivery().getCity(),
                            sale.getDelivery().getAddress(), sale.getDelivery().getNote()),
                    history.stream().map(StatusStep::from).toList());
        }
    }

    /** What the checkout shows before ordering. */
    public record ShopInfo(String currency, BigDecimal deliveryFee, BigDecimal freeDeliveryFrom) {
    }
}
