package com.financialcrm.controller;

import com.financialcrm.entity.*;
import org.springframework.data.domain.PageRequest;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.service.*;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/income")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;
    private final CustomerService customerService;
    private final InquiryService inquiryService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        var incomes = incomeService.findAll(PageRequest.of(page, size));
        model.addAttribute("incomes", incomes);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", incomes.getTotalPages());
        model.addAttribute("monthlyTotal", incomeService.getCurrentMonthTotal());
        return "income/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long customerId,
                           @RequestParam(required = false) Long inquiryId,
                           Model model) {
        Income income = new Income();
        income.setIncomeDate(LocalDate.now());
        if (customerId != null) customerService.findById(customerId).ifPresent(income::setCustomer);
        if (inquiryId != null) inquiryService.findById(inquiryId).ifPresent(income::setInquiry);
        model.addAttribute("income", income);
        model.addAttribute("categories", incomeService.findAllCategories());
        model.addAttribute("paymentModes", Income.PaymentMode.values());
        model.addAttribute("customers", customerService.findAll(PageRequest.of(0, 500)).getContent());
        return "income/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Income income,
                       @RequestParam Long categoryId,
                       @RequestParam String paymentMode,
                       @RequestParam(required = false) Long customerId,
                       @RequestParam(required = false) Long inquiryId,
                       RedirectAttributes redirectAttributes) {
        incomeService.findAllCategories().stream()
            .filter(c -> c.getId().equals(categoryId))
            .findFirst()
            .ifPresent(income::setCategory);
        income.setPaymentMode(Income.PaymentMode.valueOf(paymentMode));
        income.setCreatedBy(securityUtils.getCurrentUserOrThrow());
        if (customerId != null) customerService.findById(customerId).ifPresent(income::setCustomer);
        if (inquiryId != null) inquiryService.findById(inquiryId).ifPresent(income::setInquiry);
        incomeService.save(income);
        redirectAttributes.addFlashAttribute("successMsg", "Income recorded successfully!");
        return "redirect:/income";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Income income = incomeService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
        model.addAttribute("income", income);
        model.addAttribute("categories", incomeService.findAllCategories());
        model.addAttribute("paymentModes", Income.PaymentMode.values());
        model.addAttribute("customers", customerService.findAll(PageRequest.of(0, 500)).getContent());
        return "income/form";
    }
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        incomeService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Income record deleted.");
        return "redirect:/income";
    }
}
