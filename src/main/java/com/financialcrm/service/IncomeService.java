package com.financialcrm.service;

import com.financialcrm.entity.Income;
import com.financialcrm.entity.IncomeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncomeService {
    Income save(Income income);
    Optional<Income> findById(Long id);
    Page<Income> findAll(Pageable pageable);
    void deleteById(Long id);
    List<IncomeCategory> findAllCategories();
    BigDecimal getCurrentMonthTotal();
    List<Object[]> getMonthlyTotals(int year);
    List<Income> findByDateRange(LocalDate start, LocalDate end);
    BigDecimal sumByDateRange(LocalDate start, LocalDate end);
}
