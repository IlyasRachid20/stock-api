package com.ilyas.stockapi.shop;

import com.ilyas.stockapi.service.OrderManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Every hour, cancels the online orders nobody confirmed within 48 hours (app.shop.cancel-unconfirmed-after):
 * a fake or forgotten order must not hold stock forever. The history says "system" did it.
 */
@Component
public class UnconfirmedOrderCanceller {

    private static final Logger log = LoggerFactory.getLogger(UnconfirmedOrderCanceller.class);

    private final OrderManagementService orders;
    private final ShopProperties properties;

    public UnconfirmedOrderCanceller(OrderManagementService orders, ShopProperties properties) {
        this.orders = orders;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void cancelStaleOrders() {
        int cancelled = orders.cancelUnconfirmed(Instant.now().minus(properties.cancelUnconfirmedAfter()),
                "Not confirmed within " + properties.cancelUnconfirmedAfter().toHours() + " hours");
        if (cancelled > 0) {
            log.info("Cancelled {} online order(s) nobody confirmed in time", cancelled);
        }
    }
}
