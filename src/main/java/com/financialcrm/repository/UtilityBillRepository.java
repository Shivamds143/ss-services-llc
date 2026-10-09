package com.financialcrm.repository;

import com.financialcrm.entity.UtilityBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UtilityBillRepository extends JpaRepository<UtilityBill, Long> {

    @Query("SELECT b FROM UtilityBill b LEFT JOIN FETCH b.category " +
           "WHERE b.isActive = true ORDER BY b.dueDate ASC")
    Page<UtilityBill> findAllActive(Pageable pageable);

    @Query("SELECT b FROM UtilityBill b LEFT JOIN FETCH b.category " +
           "WHERE b.isActive = true AND b.status = 'PENDING' " +
           "AND b.dueDate <= :targetDate")
    List<UtilityBill> findDueForReminder(@Param("targetDate") LocalDate targetDate);

    @Query("SELECT b FROM UtilityBill b LEFT JOIN FETCH b.category " +
           "WHERE b.isActive = true AND b.status = 'PENDING' AND b.dueDate < :today")
    List<UtilityBill> findOverdue(@Param("today") LocalDate today);

    @Query("SELECT b FROM UtilityBill b LEFT JOIN FETCH b.category WHERE b.id = :id")
    java.util.Optional<UtilityBill> findByIdWithCategory(@Param("id") Long id);
}