package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Customer;

/** Short customer info shown inside a sale. */
public record CustomerSummary(Long id, String name) {

    public static CustomerSummary from(Customer customer) {
        return new CustomerSummary(customer.getId(), customer.getName());
    }
}
