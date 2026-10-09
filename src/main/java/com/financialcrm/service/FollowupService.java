package com.financialcrm.service;

import com.financialcrm.entity.Followup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FollowupService {
    Followup save(Followup followup);
    Optional<Followup> findById(Long id);
    List<Followup> findByInquiryId(Long inquiryId);
    List<Followup> findTodayFollowups();
    List<Followup> findOverdueFollowups();
    List<Followup> findUpcomingFollowups(int days);
    List<Followup> findByNextFollowupDate(LocalDate date);
    Page<Followup> findAll(Pageable pageable);
    void deleteById(Long id);
    long countTodayPending();
    // Auto reminder
    int sendAutoReminders();
}