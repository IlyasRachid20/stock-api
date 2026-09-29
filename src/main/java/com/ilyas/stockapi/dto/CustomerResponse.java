package com.ilyas.stockapi.dto;

import com.ilyas.stockapi.entity.Customer;

public record CustomerResponse(Long id, String name, String email, String phone) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getPhone());
    }
}
