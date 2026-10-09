package com.financialcrm.service;

import com.financialcrm.entity.UtilityBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UtilityBillService {
    UtilityBill save(UtilityBill bill);
    Optional<UtilityBill> findById(Long id);
    Page<UtilityBill> findAll(Pageable pageable);
    void deleteById(Long id);

    /** Marks bill paid, creates linked Expense record, advances due date if recurring */
    void markAsPaid(Long billId, String paymentModeStr, Long userId, String paymentReference, String paymentNotes);

    List<UtilityBill> findDueForReminder(int daysAhead);
    List<UtilityBill> findOverdue();
}
