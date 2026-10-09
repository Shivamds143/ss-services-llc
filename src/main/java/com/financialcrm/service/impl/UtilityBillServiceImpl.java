package com.financialcrm.service.impl;

import com.financialcrm.entity.Expense;
import com.financialcrm.entity.UtilityBill;
import com.financialcrm.entity.User;
import com.financialcrm.repository.UtilityBillRepository;
import com.financialcrm.repository.UserRepository;
import com.financialcrm.service.ExpenseService;
import com.financialcrm.service.UtilityBillService;
import com.financialcrm.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilityBillServiceImpl implements UtilityBillService {

    private final UtilityBillRepository utilityBillRepository;
    private final ExpenseService expenseService;
    private final UserRepository userRepository;

    @Override
    public UtilityBill save(UtilityBill bill) {
        return utilityBillRepository.save(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilityBill> findById(Long id) {
        return utilityBillRepository.findByIdWithCategory(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UtilityBill> findAll(Pageable pageable) {
        return utilityBillRepository.findAllActive(pageable);
    }

    @Override
    public void deleteById(Long id) {
        utilityBillRepository.deleteById(id);
    }

    @Override
    public void markAsPaid(Long billId, String paymentModeStr, Long userId, String paymentReference, String paymentNotes) {
        UtilityBill bill = utilityBillRepository.findByIdWithCategory(billId)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityBill", billId));

        if (bill.getStatus() == UtilityBill.BillStatus.PAID && bill.getExpense() != null) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        StringBuilder description = new StringBuilder(bill.getBillName() + " - paid via Utility Reminder");
        if (paymentReference != null && !paymentReference.isBlank()) {
            description.append(" [Ref: ").append(paymentReference).append("]");
        }

        Expense expense = Expense.builder()
                .expenseDate(LocalDate.now())
                .category(bill.getCategory().getParent() != null ? bill.getCategory().getParent() : bill.getCategory())
                .subCategory(bill.getCategory().getParent() != null ? bill.getCategory() : null)
                .amount(bill.getAmount())
                .paymentMode(Expense.PaymentMode.valueOf(paymentModeStr))
                .description(description.toString())
                .notes(paymentNotes)
                .createdBy(user)
                .build();
        expense = expenseService.save(expense);

        bill.setExpense(expense);
        bill.setStatus(UtilityBill.BillStatus.PAID);

        // If recurring, create the next cycle's bill automatically
        if (bill.getRecurrenceType() != UtilityBill.RecurrenceType.ONE_TIME) {
            UtilityBill next = UtilityBill.builder()
                    .billName(bill.getBillName())
                    .category(bill.getCategory())
                    .amount(bill.getAmount())
                    .dueDate(nextDueDate(bill.getDueDate(), bill.getRecurrenceType()))
                    .recurrenceType(bill.getRecurrenceType())
                    .reminderDaysBefore(bill.getReminderDaysBefore())
                    .status(UtilityBill.BillStatus.PENDING)
                    .notes(bill.getNotes())
                    .isActive(true)
                    .build();
            utilityBillRepository.save(next);
        }

        utilityBillRepository.save(bill);
    }

    private LocalDate nextDueDate(LocalDate current, UtilityBill.RecurrenceType type) {
        return switch (type) {
            case MONTHLY -> current.plusMonths(1);
            case QUARTERLY -> current.plusMonths(3);
            case YEARLY -> current.plusYears(1);
            case ONE_TIME -> current;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilityBill> findDueForReminder(int daysAhead) {
        return utilityBillRepository.findDueForReminder(LocalDate.now().plusDays(daysAhead));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilityBill> findOverdue() {
        return utilityBillRepository.findOverdue(null);
    }
}
