package com.ilyas.stockapi.shop;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Shop settings, and the hourly job that cancels unconfirmed orders (UnconfirmedOrderCanceller)
@Configuration
@EnableConfigurationProperties(ShopProperties.class)
@EnableScheduling
public class ShopConfig {
}
