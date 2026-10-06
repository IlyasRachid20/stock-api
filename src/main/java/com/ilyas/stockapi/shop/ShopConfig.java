package com.ilyas.stockapi.shop;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShopProperties.class)
public class ShopConfig {
}
