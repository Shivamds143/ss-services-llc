package com.financialcrm.repository;

import com.financialcrm.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.isActive = true AND " +
           "(LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(c.referenceName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> searchCustomers(@Param("search") String search, Pageable pageable);

    List<Customer> findByMobileNumber(String mobileNumber);

    @Query("SELECT c FROM Customer c WHERE MONTH(c.dateOfBirth) = MONTH(CURRENT_DATE) " +
           "AND DAY(c.dateOfBirth) = DAY(CURRENT_DATE) AND c.isActive = true")
    List<Customer> findTodayBirthdays();

    @Query("SELECT c FROM Customer c WHERE MONTH(c.anniversaryDate) = MONTH(CURRENT_DATE) " +
           "AND DAY(c.anniversaryDate) = DAY(CURRENT_DATE) AND c.isActive = true")
    List<Customer> findTodayAnniversaries();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = true AND c.inquiryDate = :date")
    long countTodayVisitors(@Param("date") LocalDate date);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = true")
    long countActiveCustomers();

    @Query("SELECT c FROM Customer c WHERE c.isActive = true AND c.inquiryDate BETWEEN :startDate AND :endDate ORDER BY c.inquiryDate DESC")
    List<Customer> findByInquiryDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

	List<Customer> findByIsActiveTrueOrderByCreatedAtDesc();
}
