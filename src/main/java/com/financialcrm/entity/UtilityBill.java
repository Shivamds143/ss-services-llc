package com.financialcrm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "utility_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityBill {

    public enum RecurrenceType {
        MONTHLY, QUARTERLY, YEARLY, ONE_TIME
    }

    public enum BillStatus {
        PENDING, PAID, OVERDUE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "bill_name", nullable = false, length = 150)
    private String billName; // e.g. "Electricity Bill - June"

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category; // links to e.g. "Electricity Bill" sub-category

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false)
    @Builder.Default
    private RecurrenceType recurrenceType = RecurrenceType.MONTHLY;

    @Column(name = "reminder_days_before")
    @Builder.Default
    private Integer reminderDaysBefore = 3;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BillStatus status = BillStatus.PENDING;

    /** Linked expense once the bill is marked paid */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}