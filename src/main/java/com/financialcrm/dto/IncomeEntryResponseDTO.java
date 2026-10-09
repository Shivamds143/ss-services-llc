package com.financialcrm.dto;

import com.financialcrm.entity.IncomeEntry;
import com.financialcrm.entity.IncomeEntry.PaymentMode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only projection of an IncomeEntry for list views and REST responses.
 * Constructed from entity in the service layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeEntryResponseDTO {

    private Long          id;
    private LocalDate     incomeDate;
    private BigDecimal    amount;
    private PaymentMode   paymentMode;
    private String        paymentModeDisplay;
    private String        transactionReference;
    private String        remarks;

    // Category info
    private Long          categoryId;
    private String        categoryName;
    private String        categoryGroup;

    // Customer info (nullable)
    private Long          customerId;
    private String        customerName;
    private String        customerMobile;

    // Employee info (nullable)
    private Long          employeeId;
    private String        employeeName;

    // Audit
    private String        createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method – maps entity to DTO.
     * Call after fetching entity with details (avoid lazy loading issues).
     */
    public static IncomeEntryResponseDTO from(IncomeEntry ie) {
        IncomeEntryResponseDTOBuilder b = IncomeEntryResponseDTO.builder()
            .id(ie.getId())
            .incomeDate(ie.getIncomeDate())
            .amount(ie.getAmount())
            .paymentMode(ie.getPaymentMode())
            .paymentModeDisplay(ie.getPaymentMode() != null ? ie.getPaymentMode().getDisplayName() : null)
            .transactionReference(ie.getTransactionReference())
            .remarks(ie.getRemarks())
            .createdAt(ie.getCreatedAt())
            .updatedAt(ie.getUpdatedAt());

        if (ie.getIncomeCategory() != null) {
            b.categoryId(ie.getIncomeCategory().getId())
             .categoryName(ie.getIncomeCategory().getCategoryName())
             .categoryGroup(ie.getIncomeCategory().getCategoryGroup());
        }

        if (ie.getCustomer() != null) {
            b.customerId(ie.getCustomer().getId())
             .customerName(ie.getCustomer().getCustomerName())
             .customerMobile(ie.getCustomer().getMobileNumber());
        }

        if (ie.getEmployee() != null) {
            b.employeeId(ie.getEmployee().getId())
             .employeeName(ie.getEmployee().getEmployeeName());
        }

        if (ie.getCreatedBy() != null) {
            b.createdByUsername(ie.getCreatedBy().getUsername());
        }

        return b.build();
    }
}
