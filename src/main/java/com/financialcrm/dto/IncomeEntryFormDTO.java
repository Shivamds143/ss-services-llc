package com.financialcrm.dto;

import com.financialcrm.entity.IncomeEntry.PaymentMode;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO bound from the Add / Edit Income Entry form.
 * Decouples HTTP form binding from the JPA entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeEntryFormDTO {

    /** Present on edit; null on create */
    private Long id;

    @NotNull(message = "Income date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate incomeDate;

    @NotNull(message = "Income category is required")
    private Long incomeCategoryId;

    /** Optional – links income to a customer */
    private Long customerId;

    /** Optional – links income to an employee */
    private Long employeeId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionReference;

    @Size(max = 2000, message = "Remarks must not exceed 2000 characters")
    private String remarks;
}
