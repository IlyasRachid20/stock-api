package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.CustomerRequest;
import com.ilyas.stockapi.dto.CustomerResponse;
import com.ilyas.stockapi.entity.Customer;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;

    public CustomerService(CustomerRepository customerRepository, SaleRepository saleRepository) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
    }

    public Page<CustomerResponse> find(String search, Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? customerRepository.findAll(pageable)
                : customerRepository.search(search.trim(), pageable);
        return page.map(CustomerResponse::from);
    }

    public CustomerResponse get(Long id) {
        return CustomerResponse.from(findOrThrow(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String email = request.email();
        if (email != null && customerRepository.existsByEmailIgnoreCase(email)) {
            throw emailAlreadyUsed(email);
        }
        Customer customer = new Customer();
        apply(request, email, customer);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findOrThrow(id);
        String email = request.email();
        if (email != null && customerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw emailAlreadyUsed(email);
        }
        apply(request, email, customer);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findOrThrow(id);
        long sales = saleRepository.countByCustomerId(id);
        if (sales > 0) {
            throw new ConflictException("Customer '" + customer.getName()
                    + "' cannot be deleted: they have " + sales + " sale(s)");
        }
        customerRepository.delete(customer);
    }

    private Customer findOrThrow(Long id) {
        return customerRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    private static void apply(CustomerRequest request, String email, Customer customer) {
        customer.setName(request.name());
        customer.setEmail(email);
        customer.setPhone(request.phone());
    }

    private static ConflictException emailAlreadyUsed(String email) {
        return new ConflictException("Email " + email + " is already used by another customer");
    }
}
