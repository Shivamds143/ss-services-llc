package com.financialcrm.service.impl;

import com.financialcrm.dto.IncomeEntryFilterDTO;
import com.financialcrm.dto.IncomeEntryFormDTO;
import com.financialcrm.dto.IncomeEntryResponseDTO;
import com.financialcrm.dto.IncomeSummaryDTO;
import com.financialcrm.entity.*;
import com.financialcrm.entity.IncomeEntry.PaymentMode;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.repository.*;
import com.financialcrm.service.IncomeEntryService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of IncomeEntryService.
 * Architecture mirrors ExpenseServiceImpl for consistency.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IncomeEntryServiceImpl implements IncomeEntryService {

    private final IncomeEntryRepository     incomeEntryRepository;
    private final IncomeCategoryRepository  categoryRepository;
    private final CustomerRepository        customerRepository;
    private final EmployeeRepository        employeeRepository;
    private final SecurityUtils             securityUtils;

    // =========================================================================
    // CRUD
    // =========================================================================

    @Override
    public IncomeEntry create(IncomeEntryFormDTO formDTO) {
        IncomeEntry entry = new IncomeEntry();
        mapFormToEntity(formDTO, entry);
        entry.setCreatedBy(securityUtils.getCurrentUserOrThrow());
        return incomeEntryRepository.save(entry);
    }

    @Override
    public IncomeEntry update(Long id, IncomeEntryFormDTO formDTO) {
        IncomeEntry existing = incomeEntryRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("IncomeEntry", id));
        mapFormToEntity(formDTO, existing);
        // createdBy is NOT changed on update
        return incomeEntryRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (!incomeEntryRepository.existsById(id)) {
            throw new ResourceNotFoundException("IncomeEntry", id);
        }
        incomeEntryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncomeEntry> findById(Long id) {
        return incomeEntryRepository.findByIdWithDetails(id);
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeEntryResponseDTO findByIdAsDTO(Long id) {
        IncomeEntry entry = incomeEntryRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("IncomeEntry", id));
        return IncomeEntryResponseDTO.from(entry);
    }

    // =========================================================================
    // Listing / Search / Filter
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<IncomeEntryResponseDTO> findAll(IncomeEntryFilterDTO filter) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        Page<IncomeEntry> page;

        if (filter.hasActiveFilter()) {
            Specification<IncomeEntry> spec = IncomeEntrySpecification.build(
            		filter.getFromDate(),
                    filter.getToDate(),
                filter.getCategoryId(),
                filter.getCategoryGroup(),
                filter.getEmployeeId(),
                filter.getPaymentMode(),
                filter.getMinAmount(),
                filter.getMaxAmount(),
                filter.getSearch()
            );
            page = incomeEntryRepository.findAll(spec, pageable);
        } else {
            page = incomeEntryRepository.findAllWithDetails(pageable);
        }

        return page.map(IncomeEntryResponseDTO::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncomeEntryResponseDTO> findAllPaged(int page, int size) {
        return incomeEntryRepository.findAllWithDetails(PageRequest.of(page, size))
                                    .map(IncomeEntryResponseDTO::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomeEntryResponseDTO> findByCustomerId(Long customerId) {
        return incomeEntryRepository.findByCustomerIdOrderByIncomeDateDesc(customerId)
                                    .stream()
                                    .map(IncomeEntryResponseDTO::from)
                                    .collect(Collectors.toList());
    }

    // =========================================================================
    // Category master
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<IncomeCategory> findAllActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByCategoryGroupAscCategoryNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<IncomeCategory>> findCategoriesGrouped() {
        return categoryRepository.findByActiveTrueOrderByCategoryGroupAscCategoryNameAsc()
            .stream()
            .collect(Collectors.groupingBy(
                IncomeCategory::getCategoryGroup,
                LinkedHashMap::new,        // preserve insertion order (alphabetical)
                Collectors.toList()
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findDistinctCategoryGroups() {
    	return categoryRepository.findDistinctCategoryGroups();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncomeCategory> findCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    // =========================================================================
    // Aggregations / Summary
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentMonthTotal() {
        BigDecimal total = incomeEntryRepository.getCurrentMonthTotal();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumByDateRange(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = incomeEntryRepository.sumByDateRange(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, BigDecimal> getMonthlyTotals(int year) {
        Map<Integer, BigDecimal> result = new LinkedHashMap<>();
        // Pre-fill all 12 months with ZERO
        for (int m = 1; m <= 12; m++) result.put(m, BigDecimal.ZERO);
        incomeEntryRepository.getMonthlyTotals(year)
            .forEach(row -> result.put(((Number) row[0]).intValue(), (BigDecimal) row[1]));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyCategoryWise(int month, int year) {
        return incomeEntryRepository.getMonthlyCategoryWise(month, year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getGroupWiseTotals(LocalDate startDate, LocalDate endDate) {
        return incomeEntryRepository.sumByGroupAndDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getEmployeeWiseTotals(LocalDate startDate, LocalDate endDate) {
        return incomeEntryRepository.sumByEmployeeAndDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeSummaryDTO getSummary(IncomeEntryFilterDTO filter) {
        LocalDate now        = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd   = now.withDayOfMonth(now.lengthOfMonth());

        // Determine effective date range for filtered totals
        LocalDate rangeStart = filter.getFromDate() != null ? filter.getFromDate() : monthStart;
        LocalDate rangeEnd   = filter.getToDate()   != null ? filter.getToDate()   : monthEnd;

        BigDecimal monthTotal    = getCurrentMonthTotal();
        long       monthCount    = incomeEntryRepository.countCurrentMonth();
        BigDecimal filteredTotal = sumByDateRange(rangeStart, rangeEnd);
        long       filteredCount = filter.hasActiveFilter()
            ? incomeEntryRepository.count(
                IncomeEntrySpecification.build(
                		filter.getFromDate(), filter.getToDate(),
                    filter.getCategoryId(), filter.getCategoryGroup(),
                    filter.getEmployeeId(), filter.getPaymentMode(),
                    filter.getMinAmount(), filter.getMaxAmount(),
                    filter.getSearch()))
            : incomeEntryRepository.count();

        return IncomeSummaryDTO.builder()
            .currentMonthTotal(monthTotal)
            .currentMonthCount(monthCount)
            .filteredTotal(filteredTotal)
            .filteredCount(filteredCount)
            .monthlyTotals(getMonthlyTotals(now.getYear()))
            .groupBreakdown(getGroupWiseTotals(rangeStart, rangeEnd))
            .build();
    }

    @Override
    public PaymentMode[] getPaymentModes() {
        return PaymentMode.values();
    }

    // ── Dashboard Graph 2: Insurance Income ───────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyInsuranceTypeTotals(int year) {
        return incomeEntryRepository.sumMonthlyByInsuranceTypes(year);
    }

    // ── Dashboard Graph 3: Mutual Fund Income ─────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyMutualFundCategoryTotals(int year) {
        return incomeEntryRepository.sumMonthlyByMutualFundCategories(year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getAllCategoryGroupTotals() {
        return incomeEntryRepository.sumByAllCategoryGroups();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /** Maps form DTO fields onto the given entity (shared between create and update). */
    private void mapFormToEntity(IncomeEntryFormDTO dto, IncomeEntry entity) {
        entity.setIncomeDate(dto.getIncomeDate());
        entity.setAmount(dto.getAmount());
        entity.setPaymentMode(dto.getPaymentMode());
        entity.setTransactionReference(
            dto.getTransactionReference() != null ? dto.getTransactionReference().trim() : null);
        entity.setRemarks(
            dto.getRemarks() != null ? dto.getRemarks().trim() : null);

        // Category (required)
        IncomeCategory category = categoryRepository.findById(dto.getIncomeCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "IncomeCategory", dto.getIncomeCategoryId()));
        entity.setIncomeCategory(category);

        // Customer (optional)
        if (dto.getCustomerId() != null) {
            customerRepository.findById(dto.getCustomerId())
                              .ifPresent(entity::setCustomer);
        } else {
            entity.setCustomer(null);
        }

        // Employee (optional)
        if (dto.getEmployeeId() != null) {
            employeeRepository.findById(dto.getEmployeeId())
                              .ifPresent(entity::setEmployee);
        } else {
            entity.setEmployee(null);
        }
    }
}
