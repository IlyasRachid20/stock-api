package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.CustomerRequest;
import com.ilyas.stockapi.dto.CustomerResponse;
import com.ilyas.stockapi.entity.Customer;
import com.ilyas.stockapi.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // GET /api/customers?search=ahmed&page=0&size=20 (search matches name or email)
    @GetMapping
    public PagedModel<CustomerResponse> getAll(
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? customerRepository.findAll(pageable)
                : customerRepository.search(search.trim(), pageable);
        return new PagedModel<>(page.map(CustomerResponse::from));
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return CustomerResponse.from(findOrThrow(id));
    }

    @PostMapping
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        if (request.email() != null && customerRepository.existsByEmail(request.email())) {
            throw emailAlreadyUsed(request.email());
        }
        Customer customer = new Customer();
        apply(request, customer);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        Customer customer = findOrThrow(id);
        if (request.email() != null && customerRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw emailAlreadyUsed(request.email());
        }
        apply(request, customer);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        customerRepository.delete(findOrThrow(id));
    }

    private Customer findOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static void apply(CustomerRequest request, Customer customer) {
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
    }

    private static ResponseStatusException emailAlreadyUsed(String email) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email " + email + " is already used by another customer");
    }
}
