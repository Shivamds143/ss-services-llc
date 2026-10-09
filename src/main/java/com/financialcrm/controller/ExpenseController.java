package com.financialcrm.controller;

import com.financialcrm.entity.*;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.service.ExpenseService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/expenses")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        var expenses = expenseService.findAll(PageRequest.of(page, size));
        model.addAttribute("expenses", expenses);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", expenses.getTotalPages());
        model.addAttribute("monthlyTotal", expenseService.getCurrentMonthTotal());
        return "expense/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Expense expense = new Expense();
        expense.setExpenseDate(LocalDate.now());
        model.addAttribute("expense", expense);
        model.addAttribute("categories", expenseService.findAllCategories());
        model.addAttribute("paymentModes", Expense.PaymentMode.values());
        return "expense/form";
    }

    /** AJAX: return sub-categories for a given parent category id */
    @GetMapping("/sub-categories")
    @ResponseBody
    public ResponseEntity<List<SubCategoryDto>> getSubCategories(@RequestParam Long parentId) {
        List<SubCategoryDto> list = expenseService.findSubCategories(parentId)
            .stream()
            .map(c -> new SubCategoryDto(c.getId(), c.getCategoryName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Expense expense,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Long subCategoryId,
                       @RequestParam String paymentMode,
                       @RequestParam(defaultValue = "category") String expenseType,
                       @RequestParam(required = false) String manualDescription,
                       RedirectAttributes redirectAttributes) {

        if ("manual" .equals(expenseType)) {
            ExpenseCategory manualCat = expenseService.findOrCreateManualCategory();
            expense.setCategory(manualCat);
            expense.setSubCategory(null);
            expense.setDescription(manualDescription != null ? manualDescription.trim() : null);
        } else {
            ExpenseCategory cat = expenseService.findCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
            expense.setCategory(cat);

            if (subCategoryId != null) {
                ExpenseCategory subCat = expenseService.findCategoryById(subCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory", subCategoryId));
                expense.setSubCategory(subCat);
            } else {
                expense.setSubCategory(null);
            }
        }

        expense.setPaymentMode(Expense.PaymentMode.valueOf(paymentMode));
        expense.setCreatedBy(securityUtils.getCurrentUserOrThrow());
        expenseService.save(expense);
        redirectAttributes.addFlashAttribute("successMsg", "Expense recorded successfully!");
        return "redirect:/expenses";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Expense expense = expenseService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        model.addAttribute("expense", expense);
        model.addAttribute("categories", expenseService.findAllCategories());
        model.addAttribute("paymentModes", Expense.PaymentMode.values());

        // Load sub-categories for selected category so dropdown is pre-populated
        if (expense.getCategory() != null) {
            model.addAttribute("subCategories",
                expenseService.findSubCategories(expense.getCategory().getId()));
        }
        return "expense/form";
    }

    @PostMapping("/{id}/save")
    public String update(@PathVariable Long id,
                         @ModelAttribute Expense expense,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam(required = false) Long subCategoryId,
                         @RequestParam String paymentMode,
                         @RequestParam(defaultValue = "category") String expenseType,
                         @RequestParam(required = false) String manualDescription,
                         RedirectAttributes redirectAttributes) {
        Expense existing = expenseService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        existing.setExpenseDate(expense.getExpenseDate());
        existing.setAmount(expense.getAmount());
        existing.setVendorName(expense.getVendorName());
        existing.setBillNumber(expense.getBillNumber());
        existing.setNotes(expense.getNotes());

        if ("manual".equals(expenseType)) {
            ExpenseCategory manualCat = expenseService.findOrCreateManualCategory();
            existing.setCategory(manualCat);
            existing.setSubCategory(null);
            existing.setDescription(manualDescription != null ? manualDescription.trim() : null);
        } else {
            existing.setDescription(expense.getDescription());
            ExpenseCategory cat = expenseService.findCategoryById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
            existing.setCategory(cat);

            if (subCategoryId != null) {
                ExpenseCategory subCat = expenseService.findCategoryById(subCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory", subCategoryId));
                existing.setSubCategory(subCat);
            } else {
                existing.setSubCategory(null);
            }
        }

        existing.setPaymentMode(Expense.PaymentMode.valueOf(paymentMode));
        expenseService.save(existing);
        redirectAttributes.addFlashAttribute("successMsg", "Expense updated successfully!");
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        expenseService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Expense deleted.");
        return "redirect:/expenses";
    }

    /** Simple DTO for AJAX sub-category response */
    public record SubCategoryDto(Long id, String name) {}
}