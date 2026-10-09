package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.OrderStatusChange;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Online orders as the staff sees them (/api/orders): with the phone, the address and who did what. */
public final class OrderResponses {

    private OrderResponses() {
    }

    /** A row of the orders list. */
    public record OrderSummary(Long id, String orderNumber, SaleStatus status, Instant placedAt, String customerName,
                               String phone, String city, int itemCount, BigDecimal total) {

        public static OrderSummary from(Sale sale) {
            return new OrderSummary(sale.getId(), sale.getOrderNumber(), sale.getStatus(), sale.getSaleDate(),
                    sale.getDelivery().getName(), sale.getDelivery().getPhone(), sale.getDelivery().getCity(),
                    sale.getItems().stream().mapToInt(item -> item.getQuantity()).sum(),
                    sale.getTotal().add(sale.getDeliveryFee()));
        }
    }

    public record Delivery(String name, String phone, String city, String address, String note) {
    }

    public record Step(SaleStatus status, String note, String changedBy, Instant changedAt) {

        static Step from(OrderStatusChange change) {
            return new Step(change.getStatus(), change.getNote(), change.getChangedBy(), change.getChangedAt());
        }
    }

    /** One order in full, with the statuses it can move to next (the buttons the staff sees). */
    public record OrderDetail(Long id, String orderNumber, SaleStatus status, Instant placedAt, Long customerId,
                              Delivery delivery, List<SaleItemResponse> items, BigDecimal subtotal,
                              BigDecimal deliveryFee, BigDecimal total, List<Step> history, List<SaleStatus> nextStatuses) {

        public static OrderDetail from(Sale sale, List<OrderStatusChange> history) {
            var d = sale.getDelivery();
            return new OrderDetail(sale.getId(), sale.getOrderNumber(), sale.getStatus(), sale.getSaleDate(),
                    sale.getCustomer().getId(),
                    new Delivery(d.getName(), d.getPhone(), d.getCity(), d.getAddress(), d.getNote()),
                    sale.getItems().stream().map(SaleItemResponse::from).toList(),
                    sale.getTotal(), sale.getDeliveryFee(), sale.getTotal().add(sale.getDeliveryFee()),
                    history.stream().map(Step::from).toList(), sale.getStatus().next());
        }
    }

    /** Body of POST /api/orders/{id}/status, e.g. {"status": "CANCELLED", "note": "Customer changed their mind"}. */
    public record StatusRequest(@NotNull SaleStatus status, @Size(max = 255) String note) {

        public StatusRequest {
            note = (note == null || note.isBlank()) ? null : note.trim();
        }
    }
}
