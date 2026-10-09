package com.financialcrm.controller;

import com.financialcrm.entity.IncomeCategory;
import com.financialcrm.repository.IncomeCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for Income Category Master management.
 * URL prefix: /income-categories
 *
 * View: templates/income/categories.html
 *
 * Security:
 *  - GET list   → all authenticated users
 *  - POST save/update/toggle → ADMIN only
 */
@Controller
@RequestMapping("/income-categories")
@RequiredArgsConstructor
public class IncomeCategoryController {

    private final IncomeCategoryRepository categoryRepository;

    // ────────────────────────────────────────────────────────────────────────
    // LIST
    // ────────────────────────────────────────────────────────────────────────

    @GetMapping
    public String list(Model model) {
        List<IncomeCategory> categories = categoryRepository
                .findAllByOrderByCategoryGroupAscCategoryNameAsc();
        List<String> groups = categoryRepository.findDistinctCategoryGroups();

        model.addAttribute("categories",     categories);
        model.addAttribute("categoryGroups", groups);
        return "income/categories";
    }

    // ────────────────────────────────────────────────────────────────────────
    // CREATE
    // ────────────────────────────────────────────────────────────────────────

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@RequestParam String categoryName,
                       @RequestParam String categoryGroup,
                       @RequestParam(required = false) String description,
                       @RequestParam(defaultValue = "false") Boolean active,
                       RedirectAttributes redirectAttributes) {

        // Check for duplicate
        if (categoryRepository.existsByCategoryNameIgnoreCase(categoryName.trim())) {
            redirectAttributes.addFlashAttribute("errorMsg",
                "Category '" + categoryName + "' already exists.");
            return "redirect:/income-categories";
        }

        IncomeCategory category = IncomeCategory.builder()
            .categoryName(categoryName.trim())
            .categoryGroup(categoryGroup.trim())
            .description(description != null ? description.trim() : null)
            .active(active)
            .build();

        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("successMsg",
            "Category '" + categoryName + "' added successfully!");
        return "redirect:/income-categories";
    }

    // ────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @RequestParam String categoryName,
                         @RequestParam String categoryGroup,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "false") Boolean active,
                         RedirectAttributes redirectAttributes) {

        IncomeCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new com.financialcrm.exception.ResourceNotFoundException("IncomeCategory", id));

        // Check for duplicate (excluding self)
        categoryRepository.findByCategoryNameIgnoreCase(categoryName.trim())
            .ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException(
                        "Category name '" + categoryName + "' is already taken.");
                }
            });

        category.setCategoryName(categoryName.trim());
        category.setCategoryGroup(categoryGroup.trim());
        category.setDescription(description != null ? description.trim() : null);
        category.setActive(active);

        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("successMsg", "Category updated successfully!");
        return "redirect:/income-categories";
    }

    // ────────────────────────────────────────────────────────────────────────
    // TOGGLE ACTIVE
    // ────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        IncomeCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new com.financialcrm.exception.ResourceNotFoundException("IncomeCategory", id));

        category.setActive(!category.getActive());
        categoryRepository.save(category);

        String status = category.getActive() ? "activated" : "deactivated";
        redirectAttributes.addFlashAttribute("successMsg",
            "Category '" + category.getCategoryName() + "' " + status + ".");
        return "redirect:/income-categories";
    }
}
