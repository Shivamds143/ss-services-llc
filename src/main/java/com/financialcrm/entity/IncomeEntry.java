package com.financialcrm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single income transaction recorded by the CRM.
 * Table: income_entries
 */
@Entity
@Table(name = "income_entries",
       indexes = {
           @Index(name = "idx_ie_income_date",   columnList = "income_date"),
           @Index(name = "idx_ie_customer",       columnList = "customer_id"),
           @Index(name = "idx_ie_category",       columnList = "income_category_id"),
           @Index(name = "idx_ie_employee",       columnList = "employee_id"),
           @Index(name = "idx_ie_payment_mode",   columnList = "payment_mode"),
           @Index(name = "idx_ie_date_category",  columnList = "income_date,income_category_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeEntry {

    // ── Payment Mode Enum ──────────────────────────────────────────────────
    public enum PaymentMode {
        CASH("Cash"),
        UPI("UPI"),
        BANK_TRANSFER("Bank Transfer"),
        CHEQUE("Cheque"),
        CREDIT_CARD("Credit Card");

        private final String displayName;

        PaymentMode(String displayName) { this.displayName = displayName; }

        public String getDisplayName() { return displayName; }
    }

    // ── Primary Key ────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Date ───────────────────────────────────────────────────────────────
    @NotNull(message = "Income date is required")
    @Column(name = "income_date", nullable = false)
    private LocalDate incomeDate;

    // ── Customer (optional) ────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // ── Category (required) ────────────────────────────────────────────────
    @NotNull(message = "Income category is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_category_id", nullable = false)
    private IncomeCategory incomeCategory;

    // ── Amount ─────────────────────────────────────────────────────────────
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount format invalid (max 10 digits, 2 decimal places)")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // ── Payment Mode ───────────────────────────────────────────────────────
    @NotNull(message = "Payment mode is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 30)
    private PaymentMode paymentMode;

    // ── Reference ──────────────────────────────────────────────────────────
    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    // ── Remarks ────────────────────────────────────────────────────────────
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // ── Employee (optional – who handled/generated this income) ───────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    // ── Audit ──────────────────────────────────────────────────────────────
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Lifecycle Hooks ────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (incomeDate == null) incomeDate = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
