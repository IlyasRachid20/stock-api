package com.ilyas.stockapi.repository;

import com.ilyas.stockapi.entity.Customer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    // Customers whose name or email contains the search text; no search means every customer (see SearchText)
    static Specification<Customer> matching(String search) {
        return (root, query, cb) -> SearchText.isBlank(search) ? cb.conjunction()
                : cb.or(SearchText.contains(cb, root.get("name"), search),
                        SearchText.contains(cb, root.get("email"), search));
    }
}