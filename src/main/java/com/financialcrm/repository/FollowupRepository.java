package com.financialcrm.repository;

import com.financialcrm.entity.Followup;
import com.financialcrm.entity.Followup.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowupRepository extends JpaRepository<Followup, Long> {

    @Query("SELECT f FROM Followup f " +
           "LEFT JOIN FETCH f.customer " +
           "LEFT JOIN FETCH f.inquiry i " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH f.createdBy " +
           "WHERE f.inquiry.id = :inquiryId " +
           "ORDER BY f.followupDate DESC")
    List<Followup> findByInquiryIdOrderByFollowupDateDesc(@Param("inquiryId") Long inquiryId);

    @Query("SELECT f FROM Followup f " +
           "JOIN FETCH f.customer " +
           "JOIN FETCH f.inquiry i " +
           "JOIN FETCH i.inquiryType " +
           "WHERE f.followupDate = :date AND f.status = 'PENDING' " +
           "ORDER BY f.followupTime ASC")
    List<Followup> findTodayFollowups(@Param("date") LocalDate date);

    @Query("SELECT f FROM Followup f WHERE f.followupDate BETWEEN :startDate AND :endDate AND f.status = 'PENDING'")
    List<Followup> findFollowupsByDateRange(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Query("SELECT f FROM Followup f " +
           "JOIN FETCH f.customer " +
           "JOIN FETCH f.inquiry i " +
           "JOIN FETCH i.inquiryType " +
           "JOIN FETCH f.createdBy " +
           "WHERE f.followupDate < :today AND f.status = 'PENDING'")
    List<Followup> findOverdueFollowups(@Param("today") LocalDate today);

    // ── AUTO REMINDER: call + whatsapp reminders not yet sent ──
    @Query("SELECT f FROM Followup f " +
           "JOIN FETCH f.customer " +
           "JOIN FETCH f.inquiry i " +
           "JOIN FETCH i.inquiryType " +
           "JOIN FETCH f.createdBy " +
           "WHERE (f.callReminder = true OR f.whatsappReminder = true) " +
           "AND f.reminderSent = false AND f.followupDate = :today " +
           "AND f.autoReminderEnabled = true")
    List<Followup> findPendingReminders(@Param("today") LocalDate today);

    // ── AUTO REMINDER: advance reminders (N days before) ──
    @Query("SELECT f FROM Followup f " +
           "JOIN FETCH f.customer " +
           "JOIN FETCH f.inquiry i " +
           "JOIN FETCH i.inquiryType " +
           "JOIN FETCH f.createdBy " +
           "WHERE f.autoReminderEnabled = true " +
           "AND f.reminderSent = false " +
           "AND f.status = 'PENDING' " +
           "AND f.followupDate > :today " +
           "AND f.reminderDaysBefore > 0")
    List<Followup> findAdvanceReminders(@Param("today") LocalDate today);

    // ── Next follow-up date reminders ──
    @Query("SELECT f FROM Followup f " +
           "JOIN FETCH f.customer " +
           "JOIN FETCH f.inquiry i " +
           "JOIN FETCH i.inquiryType " +
           "WHERE f.nextFollowupDate = :date AND f.status = 'COMPLETED'")
    List<Followup> findByNextFollowupDate(@Param("date") LocalDate date);

    @Query(value = "SELECT f FROM Followup f " +
                   "LEFT JOIN FETCH f.customer " +
                   "LEFT JOIN FETCH f.inquiry i " +
                   "LEFT JOIN FETCH i.inquiryType " +
                   "LEFT JOIN FETCH f.createdBy " +
                   "WHERE f.status = :status " +
                   "ORDER BY f.followupDate DESC",
           countQuery = "SELECT COUNT(f) FROM Followup f WHERE f.status = :status")
    Page<Followup> findByStatusOrderByFollowupDateDesc(@Param("status") Status status, Pageable pageable);

    long countByStatusAndFollowupDate(Status status, LocalDate date);

    // ── Upcoming follow-ups for next N days ──
    @Query("SELECT f FROM Followup f " +
           "JOIN FETCH f.customer " +
           "JOIN FETCH f.inquiry i " +
           "JOIN FETCH i.inquiryType " +
           "WHERE f.followupDate BETWEEN :today AND :endDate " +
           "AND f.status = 'PENDING' ORDER BY f.followupDate ASC, f.followupTime ASC")
    List<Followup> findUpcomingFollowups(@Param("today") LocalDate today,
                                          @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT f FROM Followup f " +
                   "LEFT JOIN FETCH f.customer " +
                   "LEFT JOIN FETCH f.inquiry i " +
                   "LEFT JOIN FETCH i.inquiryType " +
                   "LEFT JOIN FETCH f.createdBy " +
                   "ORDER BY f.followupDate DESC",
           countQuery = "SELECT COUNT(f) FROM Followup f")
    Page<Followup> findAll(Pageable pageable);

    @Query("SELECT f FROM Followup f " +
           "LEFT JOIN FETCH f.customer " +
           "LEFT JOIN FETCH f.inquiry i " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH f.createdBy " +
           "WHERE f.id = :id")
    Optional<Followup> findByIdWithDetails(@Param("id") Long id);
}
