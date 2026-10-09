package com.financialcrm.repository;

import com.financialcrm.entity.Expense;
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
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query(value = "SELECT e FROM Expense e " +
                   "LEFT JOIN FETCH e.category " +
                   "LEFT JOIN FETCH e.subCategory " +
                   "LEFT JOIN FETCH e.createdBy " +
                   "ORDER BY e.expenseDate DESC",
           countQuery = "SELECT COUNT(e) FROM Expense e")
    Page<Expense> findByOrderByExpenseDateDesc(Pageable pageable);

    @Query("SELECT e FROM Expense e " +
           "LEFT JOIN FETCH e.category " +
           "LEFT JOIN FETCH e.subCategory " +
           "WHERE e.id = :id")
    Optional<Expense> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT e FROM Expense e " +
           "LEFT JOIN FETCH e.category " +
           "LEFT JOIN FETCH e.subCategory " +
           "WHERE e.expenseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.expenseDate DESC")
    List<Expense> findByDateRange(@Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e " +
           "WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByDateRange(@Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    @Query("SELECT MONTH(e.expenseDate), SUM(e.amount) FROM Expense e " +
           "WHERE YEAR(e.expenseDate) = :year " +
           "GROUP BY MONTH(e.expenseDate)")
    List<Object[]> getMonthlyTotals(@Param("year") int year);

    @Query("SELECT e.category.categoryName, SUM(e.amount) " +
           "FROM Expense e " +
           "WHERE MONTH(e.expenseDate) = :month AND YEAR(e.expenseDate) = :year " +
           "GROUP BY e.category.categoryName")
    List<Object[]> getMonthlyCategoryWise(@Param("month") int month,
                                          @Param("year") int year);

    @Query("SELECT SUM(e.amount) FROM Expense e " +
           "WHERE MONTH(e.expenseDate) = MONTH(CURRENT_DATE) " +
           "AND YEAR(e.expenseDate) = YEAR(CURRENT_DATE)")
    BigDecimal getCurrentMonthTotal();
}