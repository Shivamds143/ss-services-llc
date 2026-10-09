package com.financialcrm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "followups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Followup {

    public enum Status {
        PENDING, COMPLETED, MISSED, RESCHEDULED
    }

    public enum Outcome {
        POSITIVE, NEUTRAL, NEGATIVE, NO_RESPONSE
    }

    public enum ReminderType {
        CALL, WHATSAPP, BOTH, NONE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // ── 1) NEXT FOLLOW-UP DATE ──────────────────────────────
    @Column(name = "followup_date", nullable = false)
    private LocalDate followupDate;

    @Column(name = "followup_time")
    private LocalTime followupTime;

    @Column(name = "next_followup_date")
    private LocalDate nextFollowupDate;

    @Column(name = "next_followup_time")
    private LocalTime nextFollowupTime;

    @Column(name = "next_followup_reason")
    private String nextFollowupReason;

    // ── 2) CALL REMINDER ────────────────────────────────────
    @Column(name = "call_reminder")
    @Builder.Default
    private Boolean callReminder = false;

    @Column(name = "call_reminder_time")
    private LocalTime callReminderTime;

    @Column(name = "call_reminder_sent")
    @Builder.Default
    private Boolean callReminderSent = false;

    // ── 3) WHATSAPP REMINDER ─────────────────────────────────
    @Column(name = "whatsapp_reminder")
    @Builder.Default
    private Boolean whatsappReminder = false;

    @Column(name = "whatsapp_reminder_time")
    private LocalTime whatsappReminderTime;

    @Column(name = "whatsapp_reminder_sent")
    @Builder.Default
    private Boolean whatsappReminderSent = false;

    @Column(name = "whatsapp_message_template", columnDefinition = "TEXT")
    private String whatsappMessageTemplate;

    // ── 4) NOTES SECTION ─────────────────────────────────────
    @Column(name = "followup_notes", columnDefinition = "TEXT")
    private String followupNotes;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "customer_feedback", columnDefinition = "TEXT")
    private String customerFeedback;

    // ── 5) AUTO REMINDER ─────────────────────────────────────
    @Column(name = "email_reminder")
    @Builder.Default
    private Boolean emailReminder = false;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(name = "auto_reminder_enabled")
    @Builder.Default
    private Boolean autoReminderEnabled = true;

    @Column(name = "reminder_days_before")
    @Builder.Default
    private Integer reminderDaysBefore = 0;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    // ── STATUS & OUTCOME ─────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    @Builder.Default
    private Outcome outcome = Outcome.NEUTRAL;

    // ── RELATIONS ────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

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