package com.financialcrm.config;

import com.financialcrm.entity.ExpenseCategory;
import com.financialcrm.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds default expense categories and sub-categories on first run.
 * Safe to re-run — skips any category name that already exists
 * (category_name has a global UNIQUE constraint, so duplicates
 * across different parent groups must be avoided/renamed).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExpenseCategorySeeder implements CommandLineRunner {

    private final ExpenseCategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            log.info("Expense categories already seeded ({} found). Skipping.", categoryRepository.count());
            return;
        }

        Map<String, List<String>> categoryMap = new LinkedHashMap<>();

        categoryMap.put("Office Daily Expenses", List.of(
                "Tea / Coffee", "Water", "Snacks", "Cleaning"
        ));

        categoryMap.put("Utility Expenses", List.of(
                "Electricity Bill", "Internet Bill", "Mobile Recharge", "Office Rent",
                "Credit Cards", "Office EMI", "Building Maintenance", "Office Stationary",
                "Petrol Expense (Office)", "Electrical Expense"
        ));

        categoryMap.put("Employee Expenses", List.of(
                "Salary", "Incentives", "Travel Allowance", "Petrol Expense (Employee)"
        ));

        categoryMap.put("Marketing Expenses", List.of(
                "Banner", "Facebook Ads", "Google Ads", "Visiting Cards", "Pamphlets"
        ));

        categoryMap.put("Financial Business Expenses", List.of(
                "CIBIL Charges", "Login Fees", "Bank Processing Fees", "Stamp Duty", "Legal Charges"
        ));

        categoryMap.put("Office Maintenance", List.of(
                "Furniture", "Computer Repair", "Printer Ink", "Stationery"
        ));

        int sortOrder = 0;
        int created = 0;

        for (Map.Entry<String, List<String>> entry : categoryMap.entrySet()) {
            String parentName = entry.getKey();

            ExpenseCategory parent = categoryRepository.findByCategoryName(parentName).orElse(null);
            if (parent == null) {
                parent = ExpenseCategory.builder()
                        .categoryName(parentName)
                        .categoryCode(toCode(parentName))
                        .isActive(true)
                        .sortOrder(sortOrder)
                        .build();
                parent = categoryRepository.save(parent);
                created++;
            } else {
                log.warn("Category '{}' already exists — skipping creation, reusing existing.", parentName);
            }
            sortOrder++;

            int subOrder = 0;
            for (String subName : entry.getValue()) {
                if (categoryRepository.findByCategoryName(subName).isPresent()) {
                    log.warn("Sub-category '{}' already exists — skipping duplicate.", subName);
                    subOrder++;
                    continue;
                }

                ExpenseCategory sub = ExpenseCategory.builder()
                        .categoryName(subName)
                        .categoryCode(toCode(parentName + "_" + subName))
                        .isActive(true)
                        .sortOrder(subOrder++)
                        .parent(parent)
                        .build();
                categoryRepository.save(sub);
                created++;
            }
        }

        log.info("Expense category seeding complete. {} new categories/sub-categories created.", created);
    }

    private String toCode(String name) {
        return name.toUpperCase()
                .replace("/", "_")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}