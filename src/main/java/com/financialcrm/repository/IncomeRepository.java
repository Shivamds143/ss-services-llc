package com.financialcrm.repository;

import com.financialcrm.entity.Income;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {

    @Query(value = "SELECT i FROM Income i " +
                   "LEFT JOIN FETCH i.category " +
                   "LEFT JOIN FETCH i.createdBy " +
                   "LEFT JOIN FETCH i.customer " +
                   "LEFT JOIN FETCH i.inquiry " +
                   "ORDER BY i.incomeDate DESC",
           countQuery = "SELECT COUNT(i) FROM Income i")
    Page<Income> findByOrderByIncomeDateDesc(Pageable pageable);

    @Query("SELECT i FROM Income i WHERE i.incomeDate BETWEEN :startDate AND :endDate ORDER BY i.incomeDate DESC")
    List<Income> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.incomeDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT i.category.categoryName, SUM(i.amount) FROM Income i WHERE MONTH(i.incomeDate) = :month AND YEAR(i.incomeDate) = :year GROUP BY i.category.categoryName")
    List<Object[]> getMonthlyCategoryWise(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE MONTH(i.incomeDate) = MONTH(CURRENT_DATE) AND YEAR(i.incomeDate) = YEAR(CURRENT_DATE)")
    BigDecimal getCurrentMonthTotal();

    @Query("SELECT MONTH(i.incomeDate), SUM(i.amount) FROM Income i WHERE YEAR(i.incomeDate) = :year GROUP BY MONTH(i.incomeDate)")
    List<Object[]> getMonthlyTotals(@Param("year") int year);

    @Query("SELECT i FROM Income i " +
           "LEFT JOIN FETCH i.category " +
           "LEFT JOIN FETCH i.createdBy " +
           "LEFT JOIN FETCH i.customer " +
           "LEFT JOIN FETCH i.inquiry " +
           "WHERE i.id = :id")
    Optional<Income> findByIdWithDetails(@Param("id") Long id);
}
