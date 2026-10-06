package com.ilyas.stockapi.shop;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Online shop settings (app.shop.*).
 *
 * @param deliveryFee           what delivery costs, in MAD
 * @param freeDeliveryFrom      orders of at least this amount are delivered for free
 * @param maxOrdersPerHour      orders one address (IP) may place in an hour, against fake orders
 * @param maxOpenOrdersPerPhone orders one phone number may have waiting for confirmation
 * @param cancelUnconfirmedAfter a NEW order nobody confirmed is cancelled after this, freeing its stock
 */
@ConfigurationProperties("app.shop")
public record ShopProperties(
        @DefaultValue("30.00") BigDecimal deliveryFee,
        @DefaultValue("500.00") BigDecimal freeDeliveryFrom,
        @DefaultValue("5") int maxOrdersPerHour,
        @DefaultValue("3") int maxOpenOrdersPerPhone,
        @DefaultValue("48h") Duration cancelUnconfirmedAfter) {

    public BigDecimal deliveryFeeFor(BigDecimal subtotal) {
        return subtotal.compareTo(freeDeliveryFrom) >= 0 ? BigDecimal.ZERO.setScale(2) : deliveryFee;
    }
}
