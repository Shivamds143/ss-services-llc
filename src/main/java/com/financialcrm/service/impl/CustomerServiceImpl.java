package com.financialcrm.service.impl;

import com.financialcrm.entity.Customer;
import com.financialcrm.repository.CustomerRepository;
import com.financialcrm.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Customer> findAll(Pageable pageable) {
        return customerRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Customer> search(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return findAll(pageable);
        }
        return customerRepository.searchCustomers(query, pageable);
    }

    @Override
    public void deleteById(Long id) {
        customerRepository.findById(id).ifPresent(c -> {
            c.setIsActive(false);
            customerRepository.save(c);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> findTodayBirthdays() {
        return customerRepository.findTodayBirthdays();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTodayVisitors() {
        return customerRepository.countTodayVisitors(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveCustomers() {
        return customerRepository.countActiveCustomers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> findByInquiryDateBetween(LocalDate start, LocalDate end) {
        return customerRepository.findByInquiryDateBetween(start, end);
    }
}
