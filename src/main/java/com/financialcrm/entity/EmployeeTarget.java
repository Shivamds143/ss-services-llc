package com.financialcrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_targets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id","target_month","target_year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "target_month", nullable = false)
    private Integer targetMonth;

    @Column(name = "target_year", nullable = false)
    private Integer targetYear;

    // ── Targets ───────────────────────────────────────────────
    @Column(name = "leads_target")    @Builder.Default private Integer leadsTarget    = 0;
    @Column(name = "calls_target")    @Builder.Default private Integer callsTarget    = 0;
    @Column(name = "conversion_target") @Builder.Default private Integer conversionTarget = 0;
    @Column(name = "revenue_target", precision = 12, scale = 2)
    @Builder.Default private BigDecimal revenueTarget = BigDecimal.ZERO;

    // ── Achieved ──────────────────────────────────────────────
    @Column(name = "leads_achieved")  @Builder.Default private Integer leadsAchieved  = 0;
    @Column(name = "calls_achieved")  @Builder.Default private Integer callsAchieved  = 0;
    @Column(name = "conversion_achieved") @Builder.Default private Integer conversionAchieved = 0;
    @Column(name = "revenue_achieved", precision = 12, scale = 2)
    @Builder.Default private BigDecimal revenueAchieved = BigDecimal.ZERO;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_by")
    private User setBy;

    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate()  { updatedAt = LocalDateTime.now(); }

    // ── Helpers ───────────────────────────────────────────────
    public int leadsPercent() {
        if (leadsTarget == null || leadsTarget == 0) return 0;
        return Math.min(100, (int)((leadsAchieved * 100.0) / leadsTarget));
    }
    public int callsPercent() {
        if (callsTarget == null || callsTarget == 0) return 0;
        return Math.min(100, (int)((callsAchieved * 100.0) / callsTarget));
    }
    public int conversionPercent() {
        if (conversionTarget == null || conversionTarget == 0) return 0;
        return Math.min(100, (int)((conversionAchieved * 100.0) / conversionTarget));
    }
}