package com.financialcrm.service;

import com.financialcrm.dto.IncomeEntryFilterDTO;
import com.financialcrm.dto.IncomeEntryFormDTO;
import com.financialcrm.dto.IncomeEntryResponseDTO;
import com.financialcrm.dto.IncomeSummaryDTO;
import com.financialcrm.entity.IncomeCategory;
import com.financialcrm.entity.IncomeEntry;
import com.financialcrm.entity.IncomeEntry.PaymentMode;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service contract for the Income Management Module.
 *
 * Covers:
 *   - CRUD on IncomeEntry
 *   - Category master read
 *   - Multi-filter / search / paginated listing
 *   - Aggregation / summary stats
 */
public interface IncomeEntryService {

    // ── CRUD ───────────────────────────────────────────────────────────────

    /**
     * Create a new income entry from form DTO.
     * Sets createdBy to the currently authenticated user.
     */
    IncomeEntry create(IncomeEntryFormDTO formDTO);

    /**
     * Update an existing income entry.
     * Only editable fields are updated; createdBy is preserved.
     */
    IncomeEntry update(Long id, IncomeEntryFormDTO formDTO);

    /**
     * Delete an income entry by id.
     * Throws ResourceNotFoundException if not found.
     */
    void delete(Long id);

    /**
     * Find entity by id (with associations fetched).
     */
    Optional<IncomeEntry> findById(Long id);

    /**
     * Find entity by id and map to response DTO.
     * Throws ResourceNotFoundException if not found.
     */
    IncomeEntryResponseDTO findByIdAsDTO(Long id);

    // ── Listing / Search / Filter ──────────────────────────────────────────

    /**
     * Paginated list using dynamic specification (all filters optional).
     */
    Page<IncomeEntryResponseDTO> findAll(IncomeEntryFilterDTO filter);

    /**
     * Paginated list with no filters (default view).
     */
    Page<IncomeEntryResponseDTO> findAllPaged(int page, int size);

    /**
     * All income entries linked to a specific customer.
     */
    List<IncomeEntryResponseDTO> findByCustomerId(Long customerId);

    // ── Category master ─────────────────────────────────────────────────────

    /** All active categories, sorted by group then name. */
    List<IncomeCategory> findAllActiveCategories();

    /** All active categories grouped by category_group (for grouped selects). */
    Map<String, List<IncomeCategory>> findCategoriesGrouped();

    /** All active category-group names (for filter dropdowns). */
    List<String> findDistinctCategoryGroups();

    Optional<IncomeCategory> findCategoryById(Long id);

    // ── Aggregations / Summary ──────────────────────────────────────────────

    /** Total income for the current calendar month. */
    BigDecimal getCurrentMonthTotal();

    /**
     * Total income for a date range.
     * Returns ZERO if no records found (never null).
     */
    BigDecimal sumByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Monthly totals for a given year.
     * Returns a map: month-number (1–12) → total amount.
     */
    Map<Integer, BigDecimal> getMonthlyTotals(int year);

    /**
     * Category-wise totals for a given month/year.
     * Returns List of [categoryName, BigDecimal].
     */
    List<Object[]> getMonthlyCategoryWise(int month, int year);

    /**
     * Group-wise totals for a date range.
     * Returns List of [categoryGroup, BigDecimal].
     */
    List<Object[]> getGroupWiseTotals(LocalDate startDate, LocalDate endDate);

    /**
     * Employee-wise totals for a date range.
     * Returns List of [employeeName, BigDecimal].
     */
    List<Object[]> getEmployeeWiseTotals(LocalDate startDate, LocalDate endDate);

    /**
     * Summary stats DTO for the list page header / dashboard.
     * Respects the active filter from the supplied IncomeEntryFilterDTO.
     */
    IncomeSummaryDTO getSummary(IncomeEntryFilterDTO filter);

    /** Payment mode enum values (for form dropdowns). */
    PaymentMode[] getPaymentModes();

    // ── Dashboard Graph 2: Insurance Income by Life / Health / General ─────
    /** Returns List of [month, insuranceType, BigDecimal] for insurance income. */
    List<Object[]> getMonthlyInsuranceTypeTotals(int year);

    // ── Dashboard Graph 3: Mutual Fund Income grouped by category group ────
    /** Returns List of [month, categoryName, BigDecimal] for mutual fund / SIP categories. */
    List<Object[]> getMonthlyMutualFundCategoryTotals(int year);

    /** All category-group totals (fallback for graphs). */
    List<Object[]> getAllCategoryGroupTotals();
}
