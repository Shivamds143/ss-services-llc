package com.financialcrm.service;

import com.financialcrm.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerService {
    Customer save(Customer customer);
    Optional<Customer> findById(Long id);
    Page<Customer> findAll(Pageable pageable);
    List<Customer> findAll();   // <-- ADD THIS
    Page<Customer> search(String query, Pageable pageable);
    void deleteById(Long id);
    List<Customer> findTodayBirthdays();
    long countTodayVisitors();
    long countActiveCustomers();
    List<Customer> findByInquiryDateBetween(LocalDate start, LocalDate end);
}