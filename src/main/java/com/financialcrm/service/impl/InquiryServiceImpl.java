package com.financialcrm.service.impl;

import com.financialcrm.entity.Inquiry;
import com.financialcrm.repository.InquiryRepository;
import com.financialcrm.service.InquiryService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;
    private final SecurityUtils securityUtils;

    @Override
    public Inquiry save(Inquiry inquiry) {
        if (inquiry.getId() == null) {
            inquiry.setCreatedBy(securityUtils.getCurrentUserOrThrow());
        }
        return inquiryRepository.save(inquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Inquiry> findById(Long id) {
        return inquiryRepository.findByIdWithDetails(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inquiry> findAll(Pageable pageable) {
        return inquiryRepository.findByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inquiry> search(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) return findAll(pageable);
        return inquiryRepository.searchInquiries(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inquiry> searchByStatus(String query, Inquiry.Status status, Pageable pageable) {
        boolean hasQuery  = StringUtils.hasText(query);
        boolean hasStatus = status != null;

        if (hasStatus && !hasQuery) return inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        if (hasStatus && hasQuery)  return inquiryRepository.searchInquiriesByStatus(query, status, pageable);
        if (hasQuery)               return inquiryRepository.searchInquiries(query, pageable);
        return findAll(pageable);
    }

    // NEW: today filter — handles all search/status combinations
    @Override
    @Transactional(readOnly = true)
    public Page<Inquiry> findTodayInquiries(String query, Inquiry.Status status, Pageable pageable) {
        LocalDate today   = LocalDate.now();
        boolean hasQuery  = StringUtils.hasText(query);
        boolean hasStatus = status != null;

        if (hasStatus && hasQuery)  return inquiryRepository.searchTodayInquiriesByStatus(today, query, status, pageable);
        if (hasStatus)              return inquiryRepository.findTodayInquiriesByStatus(today, status, pageable);
        if (hasQuery)               return inquiryRepository.searchTodayInquiries(today, query, pageable);
        return inquiryRepository.findTodayInquiries(today, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTodayInquiries() {
        return inquiryRepository.countTodayInquiries(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inquiry> findByCustomerId(Long customerId) {
        return inquiryRepository.findByCustomerId(customerId);
    }

    @Override
    public void deleteById(Long id) {
        inquiryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(Inquiry.Status status) {
        return inquiryRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalLeads() {
        return inquiryRepository.countTotalLeads();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countByInquiryType() {
        return inquiryRepository.countByInquiryType();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countByStatusGrouped() {
        return inquiryRepository.countByStatusGrouped();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countMonthlyByYear(int year) {
        return inquiryRepository.countMonthlyByYear(year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inquiry> findByDateRange(LocalDate start, LocalDate end) {
        return inquiryRepository.findByDateRange(start, end);
    }

    // ── Dashboard Graph 1: Loan Income — Sanctioned vs Rejected ──────────
    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyLoanIncomeByStatus(int year) {
        return inquiryRepository.sumMonthlyLoanAmountByApprovedRejected(year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getLoanCountByStatus() {
        return inquiryRepository.countLoanInquiriesByStatus();
    }
}
