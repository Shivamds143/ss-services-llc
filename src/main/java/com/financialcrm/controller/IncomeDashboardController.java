package com.financialcrm.controller;

import com.financialcrm.dto.IncomeEntryFilterDTO;
import com.financialcrm.dto.IncomeEntryResponseDTO;
import com.financialcrm.service.IncomeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Income Dashboard.
 * URL: /income-dashboard
 *
 * View: templates/income/dashboard.html
 */
@Controller
@RequestMapping("/income-dashboard")
@RequiredArgsConstructor
public class IncomeDashboardController {

    private final IncomeEntryService incomeEntryService;

    @GetMapping
    public String dashboard(Model model) {

        LocalDate today     = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart  = today.withDayOfYear(1);

        // ── Today's Income ───────────────────────────────────────────────
        BigDecimal todayIncome = incomeEntryService.sumByDateRange(today, today);
        IncomeEntryFilterDTO todayFilter = new IncomeEntryFilterDTO();
        todayFilter.setFromDate(today);
        todayFilter.setToDate(today);
        todayFilter.setSize(1000);
        long todayCount = incomeEntryService.findAll(todayFilter).getTotalElements();

        // ── Monthly Income ───────────────────────────────────────────────
        BigDecimal monthlyIncome = incomeEntryService.sumByDateRange(monthStart, today);
        IncomeEntryFilterDTO monthFilter = new IncomeEntryFilterDTO();
        monthFilter.setFromDate(monthStart);
        monthFilter.setToDate(today);
        monthFilter.setSize(1);
        long monthlyCount = incomeEntryService.findAll(monthFilter).getTotalElements();

        // ── Yearly Income ────────────────────────────────────────────────
        BigDecimal yearlyIncome = incomeEntryService.sumByDateRange(yearStart, today);
        IncomeEntryFilterDTO yearFilter = new IncomeEntryFilterDTO();
        yearFilter.setFromDate(yearStart);
        yearFilter.setToDate(today);
        yearFilter.setSize(1);
        long yearlyCount = incomeEntryService.findAll(yearFilter).getTotalElements();

        // ── Total All-Time ───────────────────────────────────────────────
        IncomeEntryFilterDTO allFilter = new IncomeEntryFilterDTO();
        allFilter.setSize(1);
        Page<IncomeEntryResponseDTO> allPage = incomeEntryService.findAll(allFilter);
        long totalCount = allPage.getTotalElements();
        // Sum all: use a wide date range
        BigDecimal totalIncome = incomeEntryService.sumByDateRange(
            LocalDate.of(2000, 1, 1), today);

        // ── Top Categories (this month) ──────────────────────────────────
        List<Object[]> topCategories = incomeEntryService
                .getMonthlyCategoryWise(today.getMonthValue(), today.getYear());

        // ── Category Doughnut Data ───────────────────────────────────────
        List<Object[]> categoryChartData = incomeEntryService
                .getGroupWiseTotals(monthStart, today);

        // ── Payment Mode Data ────────────────────────────────────────────
        // We compute from monthly category-wise data
        List<Object[]> paymentModeData = buildPaymentModeData(monthStart, today);

        // ── Monthly Trend (current year) ─────────────────────────────────
        int currentYear = today.getYear();
        Map<Integer, BigDecimal> monthlyTotals = incomeEntryService.getMonthlyTotals(currentYear);

        // Convert monthly totals to JSON-friendly map (keys as strings for Thymeleaf inline)
        Map<String, BigDecimal> monthlyTotalsJson = new HashMap<>();
        monthlyTotals.forEach((k, v) -> monthlyTotalsJson.put(String.valueOf(k), v));

        // ── Recent 10 Entries ────────────────────────────────────────────
        IncomeEntryFilterDTO recentFilter = new IncomeEntryFilterDTO();
        recentFilter.setSortBy("incomeDate");
        recentFilter.setSortDir("desc");
        recentFilter.setSize(10);
        recentFilter.setPage(0);
        List<IncomeEntryResponseDTO> recentEntries = incomeEntryService.findAll(recentFilter).getContent();

        // ── Model ────────────────────────────────────────────────────────
        model.addAttribute("todayIncome",       todayIncome);
        model.addAttribute("todayCount",         todayCount);
        model.addAttribute("monthlyIncome",      monthlyIncome);
        model.addAttribute("monthlyCount",       monthlyCount);
        model.addAttribute("yearlyIncome",       yearlyIncome);
        model.addAttribute("yearlyCount",        yearlyCount);
        model.addAttribute("totalIncome",        totalIncome);
        model.addAttribute("totalCount",         totalCount);
        model.addAttribute("topCategories",      topCategories);
        model.addAttribute("categoryChartData",  categoryChartData);
        model.addAttribute("paymentModeData",    paymentModeData);
        model.addAttribute("monthlyTotalsJson",  monthlyTotalsJson);
        model.addAttribute("recentEntries",      recentEntries);
        model.addAttribute("currentYear",        currentYear);

        return "income/dashboard";
    }

    /**
     * Build payment-mode distribution by querying all entries for date range
     * and grouping by payment mode.
     * Returns List of [displayName, total].
     */
    private List<Object[]> buildPaymentModeData(LocalDate from, LocalDate to) {
        IncomeEntryFilterDTO filter = new IncomeEntryFilterDTO();
        filter.setFromDate(from);
        filter.setToDate(to);
        filter.setSize(5000);
        filter.setPage(0);

        List<IncomeEntryResponseDTO> entries = incomeEntryService.findAll(filter).getContent();

        Map<String, BigDecimal> modeMap = new java.util.LinkedHashMap<>();
        for (IncomeEntryResponseDTO e : entries) {
            if (e.getPaymentModeDisplay() != null) {
                modeMap.merge(e.getPaymentModeDisplay(), e.getAmount(), BigDecimal::add);
            }
        }
        return modeMap.entrySet().stream()
            .map(entry -> new Object[]{ entry.getKey(), entry.getValue() })
            .collect(java.util.stream.Collectors.toList());
    }
}
