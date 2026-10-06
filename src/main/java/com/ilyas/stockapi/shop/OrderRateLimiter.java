package com.ilyas.stockapi.shop;

import com.ilyas.stockapi.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits how many orders one address (IP) can place in an hour. Fake orders would hold stock
 * until someone cancels them, so a script can't place hundreds. Kept in memory: enough for one
 * server; with several, this would move to a shared store such as Redis.
 */
@Component
public class OrderRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final int maxPerHour;
    private final Map<String, Deque<Instant>> recent = new ConcurrentHashMap<>();

    public OrderRateLimiter(ShopProperties properties) {
        this.maxPerHour = properties.maxOrdersPerHour();
    }

    public void check(String address) {
        Instant now = Instant.now();
        Deque<Instant> times = recent.computeIfAbsent(address, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(WINDOW))) {
                times.pollFirst();
            }
            if (times.size() >= maxPerHour) {
                throw new TooManyRequestsException("Too many orders from your connection: please try again later or contact us");
            }
            times.addLast(now);
        }
    }
}
