package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}