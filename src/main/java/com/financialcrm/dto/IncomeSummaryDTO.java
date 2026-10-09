package com.financialcrm.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated income summary used on the list page header and dashboard widgets.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeSummaryDTO {

    /** Sum of all income in the current calendar month */
    @Builder.Default
    private BigDecimal currentMonthTotal = BigDecimal.ZERO;

    /** Number of income entries in current month */
    private long currentMonthCount;

    /** Sum for the applied date-range filter (same as currentMonth when no filter) */
    @Builder.Default
    private BigDecimal filteredTotal = BigDecimal.ZERO;

    /** Total count of entries matching the current filter */
    private long filteredCount;

    /**
     * Monthly totals for the current year.
     * Key = month number (1–12), Value = total amount.
     */
    private Map<Integer, BigDecimal> monthlyTotals;

    /**
     * Top categories by amount for the filtered period.
     * Each entry: [categoryName (String), total (BigDecimal)]
     */
    private List<Object[]> topCategories;

    /**
     * Per-group breakdown for the filtered period.
     * Each entry: [categoryGroup (String), total (BigDecimal)]
     */
    private List<Object[]> groupBreakdown;
}
