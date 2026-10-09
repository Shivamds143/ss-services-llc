package com.financialcrm.service;

import com.financialcrm.entity.Customer;
import com.financialcrm.repository.CustomerRepository;
import com.financialcrm.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerService.
 * Uses Mockito to avoid requiring a database connection.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
            .customerName("Ravi Shankar")
            .mobileNumber("9876543210")
            .email("ravi@test.com")
            .inquiryDate(LocalDate.now())
            .isActive(true)
            .build();
        testCustomer.setId(1L);
    }

    @Test
    void save_ShouldPersistCustomer() {
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        Customer saved = customerService.save(testCustomer);

        assertThat(saved).isNotNull();
        assertThat(saved.getCustomerName()).isEqualTo("Ravi Shankar");
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    void findById_ShouldReturnCustomer_WhenExists() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        Optional<Customer> result = customerService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getCustomerName()).isEqualTo("Ravi Shankar");
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Customer> result = customerService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void search_WithSearchTerm_ShouldCallSearchQuery() {
        Page<Customer> page = new PageImpl<>(List.of(testCustomer));
        when(customerRepository.searchCustomers(eq("Ravi"), any(PageRequest.class))).thenReturn(page);

        Page<Customer> result = customerService.search("Ravi", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(customerRepository).searchCustomers(eq("Ravi"), any(PageRequest.class));
    }

    @Test
    void search_WithNullSearch_ShouldReturnAllActive() {
        Page<Customer> page = new PageImpl<>(Arrays.asList(testCustomer));
        when(customerRepository.findByIsActiveTrueOrderByCreatedAtDesc(any(PageRequest.class))).thenReturn(page);

        Page<Customer> result = customerService.search(null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(customerRepository).findByIsActiveTrueOrderByCreatedAtDesc(any(PageRequest.class));
    }

    @Test
    void search_WithBlankSearch_ShouldReturnAllActive() {
        Page<Customer> page = new PageImpl<>(List.of(testCustomer));
        when(customerRepository.findByIsActiveTrueOrderByCreatedAtDesc(any(PageRequest.class))).thenReturn(page);

        Page<Customer> result = customerService.search("  ", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(customerRepository).findByIsActiveTrueOrderByCreatedAtDesc(any(PageRequest.class));
    }

    @Test
    void deleteById_ShouldDeactivateCustomer_NotDeletePhysically() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        customerService.deleteById(1L);

        assertThat(testCustomer.getIsActive()).isFalse();
        verify(customerRepository).save(testCustomer);
        verify(customerRepository, never()).deleteById(1L);
    }

    @Test
    void countActiveCustomers_ShouldReturnCount() {
        when(customerRepository.countActiveCustomers()).thenReturn(42L);

        long count = customerService.countActiveCustomers();

        assertThat(count).isEqualTo(42L);
    }

    @Test
    void findTodayBirthdays_ShouldReturnList() {
        when(customerRepository.findTodayBirthdays()).thenReturn(List.of(testCustomer));

        List<Customer> birthdays = customerService.findTodayBirthdays();

        assertThat(birthdays).hasSize(1);
        assertThat(birthdays.get(0).getCustomerName()).isEqualTo("Ravi Shankar");
    }

    @Test
    void countTodayVisitors_ShouldReturnCount() {
        when(customerRepository.countTodayVisitors(any(LocalDate.class))).thenReturn(5L);

        long count = customerService.countTodayVisitors();

        assertThat(count).isEqualTo(5L);
    }
}
