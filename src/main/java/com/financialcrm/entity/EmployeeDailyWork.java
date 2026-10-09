package com.financialcrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_daily_work",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "work_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDailyWork {

    public enum WorkCategory {
        FOLLOW_UP,
        CALLING,
        LEAD_GENERATION,
        CUSTOMER_MEETING,
        DOCUMENTATION,
        FIELD_VISIT,
        REPORTING,
        OTHER
    }

    public enum WorkPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    public enum WorkStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // ── Daily Leads ───────────────────────────────────────────
    @Column(name = "daily_leads")
    @Builder.Default
    private Integer dailyLeads = 0;

    @Column(name = "leads_notes", columnDefinition = "TEXT")
    private String leadsNotes;

    // ── Call Records ──────────────────────────────────────────
    @Column(name = "total_calls")
    @Builder.Default
    private Integer totalCalls = 0;

    @Column(name = "connected_calls")
    @Builder.Default
    private Integer connectedCalls = 0;

    @Column(name = "not_answered")
    @Builder.Default
    private Integer notAnswered = 0;

    @Column(name = "busy_calls")
    @Builder.Default
    private Integer busyCalls = 0;

    @Column(name = "call_notes", columnDefinition = "TEXT")
    private String callNotes;

    // ── Work Summary ──────────────────────────────────────────
    @Column(name = "work_summary", columnDefinition = "TEXT")
    private String workSummary;

    @Column(name = "work_title", length = 200)
    private String workTitle;

    @Column(name = "work_description", columnDefinition = "TEXT")
    private String workDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_category", length = 50)
    @Builder.Default
    private WorkCategory workCategory = WorkCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_priority", length = 20)
    @Builder.Default
    private WorkPriority workPriority = WorkPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_status", length = 20)
    @Builder.Default
    private WorkStatus workStatus = WorkStatus.PENDING;

    @Column(name = "work_remarks", columnDefinition = "TEXT")
    private String workRemarks;

    @Column(name = "expected_calls_target")
    @Builder.Default
    private Integer expectedCallsTarget = 0;

    @Column(name = "expected_leads_target")
    @Builder.Default
    private Integer expectedLeadsTarget = 0;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "work_progress", length = 255)
    private String workProgress;

    @Column(name = "completion_percentage")
    @Builder.Default
    private Integer completionPercentage = 0;

    @ManyToOne
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "assigned_date_time")
    private LocalDateTime assignedDateTime;

    @Column(name = "completed_date_time")
    private LocalDateTime completedDateTime;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "employee_notes", columnDefinition = "TEXT")
    private String employeeNotes;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (dueDate == null) {
            dueDate = workDate;
        }
        if (completionPercentage == null) {
            completionPercentage = 0;
        }
        if (deleted == null) {
            deleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper: call connection rate %
    public int getConnectionRate() {
        if (totalCalls == null || totalCalls == 0) return 0;
        return (int) Math.round((connectedCalls * 100.0) / totalCalls);
    }

    public boolean isOverdue() {
        return !Boolean.TRUE.equals(deleted)
                && workStatus != WorkStatus.COMPLETED
                && dueDate != null
                && dueDate.isBefore(LocalDate.now());
    }

    public int getSafeCompletionPercentage() {
        if (completionPercentage == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, completionPercentage));
    }
}
