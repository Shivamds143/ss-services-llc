package com.financialcrm.service.impl;

import com.financialcrm.entity.Expense;
import com.financialcrm.entity.ExpenseCategory;
import com.financialcrm.repository.ExpenseCategoryRepository;
import com.financialcrm.repository.ExpenseRepository;
import com.financialcrm.service.ExpenseService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;

    @Override
    public Expense save(Expense expense) {
        if (expense.getId() == null) {
            expense.setCreatedBy(securityUtils.getCurrentUserOrThrow());
        }
        return expenseRepository.save(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Expense> findById(Long id) {
        return expenseRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Expense> findAll(Pageable pageable) {
        return expenseRepository.findByOrderByExpenseDateDesc(pageable);
    }

    @Override
    public void deleteById(Long id) {
        expenseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategory> findAllCategories() {
        return categoryRepository.findByIsActiveTrueAndParentIsNullOrderBySortOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategory> findSubCategories(Long parentId) {
        return categoryRepository.findActiveSubCategoriesByParentId(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExpenseCategory> findCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentMonthTotal() {
        BigDecimal total = expenseRepository.getCurrentMonthTotal();
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyCategoryWise(int month, int year) {
        return expenseRepository.getMonthlyCategoryWise(month, year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyTotals(int year) {
        return expenseRepository.getMonthlyTotals(year);
    }

    @Override
    @Transactional
    public ExpenseCategory findOrCreateManualCategory() {
        return categoryRepository.findByCategoryCode("MANUAL_EXPENSE")
                .or(() -> categoryRepository.findByCategoryName("Manual Expense"))
                .orElseGet(() -> categoryRepository.save(
                        ExpenseCategory.builder()
                                .categoryName("Manual Expense")
                                .categoryCode("MANUAL_EXPENSE")
                                .isActive(true)
                                .sortOrder(999)
                                .build()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> findByDateRange(LocalDate start, LocalDate end) {
        return expenseRepository.findByDateRange(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumByDateRange(LocalDate start, LocalDate end) {
        BigDecimal total = expenseRepository.sumByDateRange(start, end);
        return total == null ? BigDecimal.ZERO : total;
    }
}