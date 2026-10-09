package com.financialcrm.controller;

import com.financialcrm.entity.Expense;
import com.financialcrm.entity.UtilityBill;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.service.ExpenseService;
import com.financialcrm.service.PhonePePaymentService;
import com.financialcrm.service.UtilityBillService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/utility-bills")
@RequiredArgsConstructor
public class UtilityBillController {

    private final UtilityBillService utilityBillService;
    private final ExpenseService expenseService;
    private final PhonePePaymentService phonePePaymentService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Model model) {
        var bills = utilityBillService.findAll(PageRequest.of(page, size));
        model.addAttribute("bills", bills);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bills.getTotalPages());
        model.addAttribute("overdue", utilityBillService.findOverdue());
        model.addAttribute("upcoming", utilityBillService.findDueForReminder(7));
        model.addAttribute("phonePeEnabled", phonePePaymentService.isConfigured());
        model.addAttribute("paymentModes", Expense.PaymentMode.values());
        return "utilitybill/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        UtilityBill bill = new UtilityBill();
        bill.setDueDate(LocalDate.now().plusDays(1));
        model.addAttribute("bill", bill);
        model.addAttribute("categories", flattenCategories());
        model.addAttribute("recurrenceTypes", UtilityBill.RecurrenceType.values());
        return "utilitybill/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UtilityBill bill = utilityBillService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityBill", id));
        model.addAttribute("bill", bill);
        model.addAttribute("categories", flattenCategories());
        model.addAttribute("recurrenceTypes", UtilityBill.RecurrenceType.values());
        return "utilitybill/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute UtilityBill bill,
                        @RequestParam Long categoryId,
                        RedirectAttributes redirectAttributes) {
        var cat = expenseService.findCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        bill.setCategory(cat);
        bill.setStatus(UtilityBill.BillStatus.PENDING);
        utilityBillService.save(bill);
        redirectAttributes.addFlashAttribute("successMsg", "Utility bill saved!");
        return "redirect:/utility-bills";
    }

    @PostMapping("/{id}/save")
    public String update(@PathVariable Long id,
                          @ModelAttribute UtilityBill bill,
                          @RequestParam Long categoryId,
                          RedirectAttributes redirectAttributes) {
        UtilityBill existing = utilityBillService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityBill", id));

        var cat = expenseService.findCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        existing.setBillName(bill.getBillName());
        existing.setCategory(cat);
        existing.setAmount(bill.getAmount());
        existing.setDueDate(bill.getDueDate());
        existing.setRecurrenceType(bill.getRecurrenceType());
        existing.setReminderDaysBefore(bill.getReminderDaysBefore());
        existing.setNotes(bill.getNotes());

        utilityBillService.save(existing);
        redirectAttributes.addFlashAttribute("successMsg", "Utility bill updated!");
        return "redirect:/utility-bills";
    }

    @PostMapping("/{id}/mark-paid")
    public String markPaid(@PathVariable Long id,
                            @RequestParam String paymentMode,
                            RedirectAttributes redirectAttributes) {
        try {
            Expense.PaymentMode mode = Expense.PaymentMode.valueOf(paymentMode);
            Long userId = securityUtils.getCurrentUserOrThrow().getId();
            utilityBillService.markAsPaid(id, mode.name(), userId, null, "Marked paid manually from Utility Bill screen.");
            redirectAttributes.addFlashAttribute("successMsg", "Bill marked as paid and recorded as expense.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please choose a valid payment mode.");
        }
        return "redirect:/utility-bills";
    }

    @PostMapping("/{id}/pay/phonepe")
    public String startPhonePePayment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        UtilityBill bill = utilityBillService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityBill", id));

        if (bill.getStatus() == UtilityBill.BillStatus.PAID) {
            redirectAttributes.addFlashAttribute("infoMsg", "This bill is already marked as paid.");
            return "redirect:/utility-bills";
        }

        try {
            Long userId = securityUtils.getCurrentUserOrThrow().getId();
            var result = phonePePaymentService.initiateUtilityBillPayment(bill, userId);
            return "redirect:" + result.redirectUrl();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
            return "redirect:/utility-bills";
        }
    }

    @GetMapping("/{id}/phonepe/return")
    public String handlePhonePeReturn(@PathVariable Long id,
                                      @RequestParam("tx") String merchantTransactionId,
                                      @RequestParam(value = "uid", required = false) Long userId,
                                      RedirectAttributes redirectAttributes) {
        finalizePhonePePayment(id, merchantTransactionId, userId, redirectAttributes);
        return "redirect:/utility-bills";
    }

    @PostMapping("/{id}/phonepe/callback")
    @ResponseBody
    public ResponseEntity<String> handlePhonePeCallback(@PathVariable Long id,
                                                        @RequestParam("tx") String merchantTransactionId,
                                                        @RequestParam("uid") Long userId) {
        finalizePhonePePayment(id, merchantTransactionId, userId, null);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        utilityBillService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Utility bill deleted.");
        return "redirect:/utility-bills";
    }

    private List<com.financialcrm.entity.ExpenseCategory> flattenCategories() {
        // flatten all categories + their sub-categories into one list for selection
        List<com.financialcrm.entity.ExpenseCategory> result = new java.util.ArrayList<>();
        for (var top : expenseService.findAllCategories()) {
            result.add(top);
            result.addAll(expenseService.findSubCategories(top.getId()));
        }
        return result;
    }

    private void finalizePhonePePayment(Long billId,
                                        String merchantTransactionId,
                                        Long fallbackUserId,
                                        RedirectAttributes redirectAttributes) {
        try {
            if (!isExpectedBillTransaction(billId, merchantTransactionId)) {
                if (redirectAttributes != null) {
                    redirectAttributes.addFlashAttribute("errorMsg", "Payment reference does not match this bill.");
                }
                return;
            }

            var status = phonePePaymentService.checkPaymentStatus(merchantTransactionId);
            String state = status.state() != null ? status.state().toUpperCase() : "UNKNOWN";

            if ("COMPLETED".equals(state)) {
                Long userId = fallbackUserId != null ? fallbackUserId : securityUtils.getCurrentUserOrThrow().getId();
                String paymentMode = mapPhonePeInstrumentToExpenseMode(status.paymentInstrumentType());
                String notes = "Gateway: PhonePe, State: " + state + ", Code: " + status.responseCode();
                utilityBillService.markAsPaid(billId, paymentMode, userId, status.referenceId(), notes);
                if (redirectAttributes != null) {
                    redirectAttributes.addFlashAttribute("successMsg", "PhonePe payment verified and bill recorded as expense.");
                }
                return;
            }

            if ("PENDING".equals(state) && redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("infoMsg", "PhonePe payment is still pending. Please check again shortly.");
                return;
            }

            if (redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("errorMsg",
                        "PhonePe payment did not complete. Status: " + state + ". " + status.message());
            }
        } catch (Exception ex) {
            if (redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
            }
        }
    }

    private String mapPhonePeInstrumentToExpenseMode(String instrumentType) {
        if (instrumentType == null) {
            return Expense.PaymentMode.UPI.name();
        }

        return switch (instrumentType.toUpperCase()) {
            case "UPI", "UPI_COLLECT", "UPI_INTENT", "UPI_QR" -> Expense.PaymentMode.UPI.name();
            case "CARD", "SAVED_CARD" -> Expense.PaymentMode.CARD.name();
            case "NETBANKING" -> Expense.PaymentMode.BANK_TRANSFER.name();
            default -> Expense.PaymentMode.UPI.name();
        };
    }

    private boolean isExpectedBillTransaction(Long billId, String merchantTransactionId) {
        return merchantTransactionId != null && merchantTransactionId.startsWith("UB" + billId + "-");
    }
}
