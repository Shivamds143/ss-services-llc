package com.financialcrm.dto;

import com.financialcrm.entity.IncomeEntry.PaymentMode;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeEntryFilterDTO {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    /** income_categories.id */
    private Long categoryId;

    /** income_categories.category_group */
    private String categoryGroup;

    /** employees.id */
    private Long employeeId;

    /** PaymentMode enum value */
    private PaymentMode paymentMode;

    /** Amount range filters */
    private java.math.BigDecimal minAmount;
    private java.math.BigDecimal maxAmount;

    /** Free-text search */
    private String search;

    /** Pagination */
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    /** Sorting */
    private String sortBy;
    private String sortDir;

    /** Convenience: true when at least one filter is active */
    public boolean hasActiveFilter() {
        return fromDate    != null
            || toDate      != null
            || categoryId  != null
            || (categoryGroup != null && !categoryGroup.isBlank())
            || employeeId  != null
            || paymentMode != null
            || minAmount   != null
            || maxAmount   != null
            || (search     != null && !search.isBlank());
    }
}