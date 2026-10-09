package com.financialcrm.service.impl;

import com.financialcrm.entity.Income;
import com.financialcrm.entity.IncomeCategory;
import com.financialcrm.repository.IncomeCategoryRepository;
import com.financialcrm.repository.IncomeRepository;
import com.financialcrm.service.IncomeService;
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
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepository;
    private final IncomeCategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;

    @Override
    public Income save(Income income) {
        if (income.getId() == null) {
            income.setCreatedBy(securityUtils.getCurrentUserOrThrow());
        }
        return incomeRepository.save(income);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Income> findById(Long id) {
        return incomeRepository.findByIdWithDetails(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Income> findAll(Pageable pageable) {
        return incomeRepository.findByOrderByIncomeDateDesc(pageable);
    }

    @Override
    public void deleteById(Long id) {
        incomeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomeCategory> findAllCategories() {
        return categoryRepository.findByActiveTrueOrderByCategoryNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentMonthTotal() {
        BigDecimal total = incomeRepository.getCurrentMonthTotal();
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyTotals(int year) {
        return incomeRepository.getMonthlyTotals(year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Income> findByDateRange(LocalDate start, LocalDate end) {
        return incomeRepository.findByDateRange(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumByDateRange(LocalDate start, LocalDate end) {
        BigDecimal total = incomeRepository.sumByDateRange(start, end);
        return total == null ? BigDecimal.ZERO : total;
    }
}