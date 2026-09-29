package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("select c from Customer c where lower(c.name) like lower(concat('%', :text, '%'))"
            + " or lower(c.email) like lower(concat('%', :text, '%'))")
    Page<Customer> search(String text, Pageable pageable);
}