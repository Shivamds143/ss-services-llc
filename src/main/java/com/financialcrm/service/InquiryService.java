package com.financialcrm.service;

import com.financialcrm.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InquiryService {
    Inquiry save(Inquiry inquiry);
    Optional<Inquiry> findById(Long id);
    Page<Inquiry> findAll(Pageable pageable);
    Page<Inquiry> search(String query, Pageable pageable);
    Page<Inquiry> searchByStatus(String query, Inquiry.Status status, Pageable pageable);
    // NEW: today filter — supports search + status combinations
    Page<Inquiry> findTodayInquiries(String query, Inquiry.Status status, Pageable pageable);
    long countTodayInquiries();
    List<Inquiry> findByCustomerId(Long customerId);
    void deleteById(Long id);
    long countByStatus(Inquiry.Status status);
    long countTotalLeads();
    List<Object[]> countByInquiryType();
    List<Object[]> countByStatusGrouped();
    List<Object[]> countMonthlyByYear(int year);
    List<Inquiry> findByDateRange(LocalDate start, LocalDate end);

    // ── Dashboard Graph 1: Loan Income — Sanctioned vs Rejected ──────────
    List<Object[]> getMonthlyLoanIncomeByStatus(int year);

    // ── Loan inquiry count by all statuses (for breakdown chart) ──────────
    List<Object[]> getLoanCountByStatus();
}
