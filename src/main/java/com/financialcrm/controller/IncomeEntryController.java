package com.financialcrm.controller;

import com.financialcrm.dto.*;
import com.financialcrm.entity.IncomeEntry;
import com.financialcrm.entity.IncomeEntry.PaymentMode;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.service.CustomerService;
import com.financialcrm.service.EmployeeService;
import com.financialcrm.service.IncomeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller for the Income Management Module.
 * URL prefix: /income-entries
 *
 * Replaces the old thin IncomeController – rename or remove that class
 * and update all navigation links to /income-entries.
 *
 * All views expected under src/main/resources/templates/income/
 *   - list.html  (Phase 2)
 *   - form.html  (Phase 2)
 *   - view.html  (Phase 2)
 */
@Controller
@RequestMapping("/income-entries")
@RequiredArgsConstructor
public class IncomeEntryController {

    private final IncomeEntryService incomeEntryService;
    private final CustomerService    customerService;
    private final EmployeeService    employeeService;

    // =========================================================================
    // LIST with filter / search / pagination
    // =========================================================================

    @GetMapping
    public String list(@ModelAttribute IncomeEntryFilterDTO filter, Model model) {

        Page<IncomeEntryResponseDTO> page = incomeEntryService.findAll(filter);
        IncomeSummaryDTO summary = incomeEntryService.getSummary(filter);

        model.addAttribute("incomes",        page.getContent());
        model.addAttribute("page",           page);
        model.addAttribute("currentPage",    page.getNumber());
        model.addAttribute("totalPages",     page.getTotalPages());
        model.addAttribute("totalElements",  page.getTotalElements());
        model.addAttribute("filter",         filter);
        model.addAttribute("summary",        summary);
        model.addAttribute("categories",     incomeEntryService.findAllActiveCategories());
        model.addAttribute("categoryGroups", incomeEntryService.findDistinctCategoryGroups());
        model.addAttribute("employees",      employeeService.findAll());
        model.addAttribute("paymentModes",   PaymentMode.values());

        return "income/list";
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long customerId,
                          @RequestParam(required = false) Long employeeId,
                          Model model) {
        IncomeEntryFormDTO formDTO = IncomeEntryFormDTO.builder()
            .incomeDate(LocalDate.now())
            .customerId(customerId)
            .employeeId(employeeId)
            .build();

        populateFormModel(model, formDTO);
        return "income/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("formDTO") IncomeEntryFormDTO formDTO,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateFormModel(model, formDTO);
            return "income/form";
        }

        try {
            IncomeEntry saved = incomeEntryService.create(formDTO);
            redirectAttributes.addFlashAttribute("successMsg",
                "Income entry #" + saved.getId() + " recorded successfully!");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }

        return "redirect:/income-entries";
    }

    // =========================================================================
    // VIEW
    // =========================================================================

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        IncomeEntryResponseDTO dto = incomeEntryService.findByIdAsDTO(id);
        model.addAttribute("income", dto);
        return "income/view";
    }

    // =========================================================================
    // EDIT / UPDATE
    // =========================================================================

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        IncomeEntry entity = incomeEntryService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("IncomeEntry", id));

        // Map entity → form DTO for pre-population
        IncomeEntryFormDTO formDTO = IncomeEntryFormDTO.builder()
            .id(entity.getId())
            .incomeDate(entity.getIncomeDate())
            .incomeCategoryId(entity.getIncomeCategory() != null ? entity.getIncomeCategory().getId() : null)
            .customerId(entity.getCustomer()  != null ? entity.getCustomer().getId()  : null)
            .employeeId(entity.getEmployee()  != null ? entity.getEmployee().getId()  : null)
            .amount(entity.getAmount())
            .paymentMode(entity.getPaymentMode())
            .transactionReference(entity.getTransactionReference())
            .remarks(entity.getRemarks())
            .build();

        populateFormModel(model, formDTO);
        model.addAttribute("editMode", true);
        return "income/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("formDTO") IncomeEntryFormDTO formDTO,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        formDTO.setId(id);

        if (bindingResult.hasErrors()) {
            populateFormModel(model, formDTO);
            model.addAttribute("editMode", true);
            return "income/form";
        }

        try {
            incomeEntryService.update(id, formDTO);
            redirectAttributes.addFlashAttribute("successMsg", "Income entry updated successfully!");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }

        return "redirect:/income-entries";
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            incomeEntryService.delete(id);
            redirectAttributes.addFlashAttribute("successMsg", "Income entry deleted successfully.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/income-entries";
    }

    // =========================================================================
    // AJAX endpoints
    // =========================================================================

    /**
     * AJAX: categories grouped by group-name for progressive disclosure in form.
     * GET /income-entries/api/categories
     */
    @GetMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<Map<String, ?>> getCategoriesGrouped() {
        return ResponseEntity.ok(incomeEntryService.findCategoriesGrouped());
    }

    /**
     * AJAX: monthly totals for charts.
     * GET /income-entries/api/monthly-totals?year=2026
     */
    @GetMapping("/api/monthly-totals")
    @ResponseBody
    public ResponseEntity<?> getMonthlyTotals(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return ResponseEntity.ok(incomeEntryService.getMonthlyTotals(year));
    }

    /**
     * AJAX: income entries for a specific customer (shown in customer detail page).
     * GET /income-entries/api/by-customer/{customerId}
     */
    @GetMapping("/api/by-customer/{customerId}")
    @ResponseBody
    public ResponseEntity<?> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(incomeEntryService.findByCustomerId(customerId));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void populateFormModel(Model model, IncomeEntryFormDTO formDTO) {
        model.addAttribute("formDTO",         formDTO);
        model.addAttribute("editMode",        formDTO != null && formDTO.getId() != null);
        model.addAttribute("categoriesGrouped", incomeEntryService.findCategoriesGrouped());
        model.addAttribute("categories",      incomeEntryService.findAllActiveCategories());
        model.addAttribute("paymentModes",    PaymentMode.values());
        model.addAttribute("customers",       customerService.findAll());
        model.addAttribute("employees",       employeeService.findAll());
    }
}