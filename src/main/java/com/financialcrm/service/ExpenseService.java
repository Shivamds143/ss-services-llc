package com.financialcrm.service;

import com.financialcrm.entity.Expense;
import com.financialcrm.entity.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseService {
    Expense save(Expense expense);
    Optional<Expense> findById(Long id);
    Page<Expense> findAll(Pageable pageable);
    void deleteById(Long id);

    /** Top-level categories only */
    List<ExpenseCategory> findAllCategories();

    /** Sub-categories for a given parent category id */
    List<ExpenseCategory> findSubCategories(Long parentId);

    Optional<ExpenseCategory> findCategoryById(Long id);

    BigDecimal getCurrentMonthTotal();
    List<Object[]> getMonthlyCategoryWise(int month, int year);
    List<Object[]> getMonthlyTotals(int year);
    ExpenseCategory findOrCreateManualCategory();
    List<Expense> findByDateRange(LocalDate start, LocalDate end);
    BigDecimal sumByDateRange(LocalDate start, LocalDate end);
}