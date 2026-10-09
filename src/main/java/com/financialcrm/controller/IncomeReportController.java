package com.financialcrm.controller;

import com.financialcrm.dto.IncomeEntryFilterDTO;
import com.financialcrm.dto.IncomeEntryResponseDTO;
import com.financialcrm.entity.IncomeCategory;
import com.financialcrm.service.EmployeeService;
import com.financialcrm.service.IncomeEntryService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Income Report Controller.
 * URL: /income-reports
 *
 * View: templates/income/reports.html
 *
 * Security: ADMIN + MANAGER only (method-level)
 */
@Controller
@RequestMapping("/income-reports")
public class IncomeReportController {

    private final IncomeEntryService incomeEntryService;
    private final EmployeeService    employeeService;

    public IncomeReportController(IncomeEntryService incomeEntryService,
                                  EmployeeService employeeService) {
        this.incomeEntryService = incomeEntryService;
        this.employeeService    = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST')")
    public String report(@RequestParam(defaultValue = "daily")    String  reportType,
                         @RequestParam(required = false)           LocalDate fromDate,
                         @RequestParam(required = false)           LocalDate toDate,
                         @RequestParam(required = false)           Integer  month,
                         @RequestParam(required = false)           Integer  year,
                         @RequestParam(required = false)           Long     employeeId,
                         @RequestParam(required = false)           Long     categoryId,
                         Model model) {

        int currentYear = LocalDate.now().getYear();
        if (year == null) year = currentYear;

        // ── Compute default date range based on report type ──────────────
        LocalDate[] range = getDateRange(reportType, fromDate, toDate, month, year);
        fromDate = range[0];
        toDate   = range[1];

        // ── Build filter ─────────────────────────────────────────────────
        IncomeEntryFilterDTO filter = new IncomeEntryFilterDTO();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setSortBy("incomeDate");
        filter.setSortDir("asc");
        filter.setSize(5000);   // reports can be large
        filter.setPage(0);

        if ("employee".equals(reportType) && employeeId != null) {
            filter.setEmployeeId(employeeId);
        }
        if ("category".equals(reportType) && categoryId != null) {
            filter.setCategoryId(categoryId);
        }

        Page<IncomeEntryResponseDTO> page = incomeEntryService.findAll(filter);
        List<IncomeEntryResponseDTO> entries = page.getContent();

        // ── Summary calculation ──────────────────────────────────────────
        BigDecimal totalIncome = entries.stream()
            .map(IncomeEntryResponseDTO::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalEntries = entries.size();
        BigDecimal avgIncome = totalEntries > 0
            ? totalIncome.divide(BigDecimal.valueOf(totalEntries), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        String topCategory = entries.stream()
            .filter(e -> e.getCategoryName() != null)
            .collect(Collectors.groupingBy(
                IncomeEntryResponseDTO::getCategoryName,
                Collectors.reducing(BigDecimal.ZERO, IncomeEntryResponseDTO::getAmount, BigDecimal::add)))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");

        ReportSummary summary = new ReportSummary(totalIncome, totalEntries, avgIncome, topCategory);

        // ── Chart Data ───────────────────────────────────────────────────
        ChartData chartData = buildChartData(reportType, entries, fromDate, toDate, year, month);

        // ── Model ────────────────────────────────────────────────────────
        model.addAttribute("reportType",    reportType);
        model.addAttribute("reportEntries", entries);
        model.addAttribute("reportSummary", summary);
        model.addAttribute("reportChartData", chartData);
        model.addAttribute("fromDate",      fromDate);
        model.addAttribute("toDate",        toDate);
        model.addAttribute("month",         month);
        model.addAttribute("year",          year);
        model.addAttribute("employeeId",    employeeId);
        model.addAttribute("categoryId",    categoryId);
        model.addAttribute("employees",     employeeService.findAll());
        model.addAttribute("categories",    incomeEntryService.findAllActiveCategories());
        model.addAttribute("currentYear",   currentYear);

        return "income/reports";
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private LocalDate[] getDateRange(String type, LocalDate from, LocalDate to,
                                     Integer month, int year) {
        LocalDate today = LocalDate.now();
        if (from != null && to != null) return new LocalDate[]{from, to};

        switch (type) {
            case "daily":
                return new LocalDate[]{today, today};
            case "weekly":
                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEnd   = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                return new LocalDate[]{weekStart, weekEnd};
            case "monthly":
                int m = (month != null) ? month : today.getMonthValue();
                LocalDate mStart = LocalDate.of(year, m, 1);
                LocalDate mEnd   = mStart.with(TemporalAdjusters.lastDayOfMonth());
                return new LocalDate[]{mStart, mEnd};
            case "yearly":
                return new LocalDate[]{
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31)
                };
            case "employee":
            case "category":
            case "payment":
                LocalDate defStart = today.withDayOfMonth(1);
                return new LocalDate[]{defStart, today};
            default:
                return new LocalDate[]{today.withDayOfMonth(1), today};
        }
    }

    private ChartData buildChartData(String type, List<IncomeEntryResponseDTO> entries,
                                     LocalDate from, LocalDate to, int year, Integer month) {
        List<String>     labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        switch (type) {
            case "daily": {
                // Group by date
                Map<LocalDate, BigDecimal> grouped = entries.stream()
                    .collect(Collectors.groupingBy(
                        IncomeEntryResponseDTO::getIncomeDate,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, IncomeEntryResponseDTO::getAmount, BigDecimal::add)));
                grouped.forEach((d, v) -> {
                    labels.add(d.toString());
                    values.add(v);
                });
                break;
            }
            case "weekly": {
                Map<DayOfWeek, BigDecimal> grouped = entries.stream()
                    .collect(Collectors.groupingBy(
                        e -> e.getIncomeDate().getDayOfWeek(),
                        Collectors.reducing(BigDecimal.ZERO, IncomeEntryResponseDTO::getAmount, BigDecimal::add)));
                Arrays.stream(DayOfWeek.values()).forEach(d -> {
                    labels.add(d.name().substring(0, 3));
                    values.add(grouped.getOrDefault(d, BigDecimal.ZERO));
                });
                break;
            }
            case "monthly": {
                Map<String, BigDecimal> grouped = entries.stream()
                    .collect(Collectors.groupingBy(
                        e -> e.getIncomeDate().toString().substring(0, 7),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, IncomeEntryResponseDTO::getAmount, BigDecimal::add)));
                grouped.forEach((d, v) -> { labels.add(d); values.add(v); });
                break;
            }
            case "yearly": {
                // Group by month
                String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
                Map<Integer, BigDecimal> grouped = entries.stream()
                    .collect(Collectors.groupingBy(
                        e -> e.getIncomeDate().getMonthValue(),
                        Collectors.reducing(BigDecimal.ZERO, IncomeEntryResponseDTO::getAmount, BigDecimal::add)));
                for (int i = 1; i <= 12; i++) {
                    labels.add(months[i - 1]);
                    values.add(grouped.getOrDefault(i, BigDecimal.ZERO));
                }
                break;
            }
            case "category": {
                Map<String, BigDecimal> grouped = entries.stream()
                    .filter(e -> e.getCategoryName() != null)
                    .collect(Collectors.groupingBy(
                        IncomeEntryResponseDTO::getCategoryName,
                        Collectors.reducing(BigDecimal.ZERO, IncomeEntryResponseDTO::getAmount, BigDecimal::add)));
                grouped.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .forEach(e -> { labels.add(e.getKey()); values.add(e.getValue()); });
                break;
            }
            case "employee": {
                Map<String, BigDecimal> grouped = entries.stream()
                    .filter(e -> e.getEmployeeName() != null)
                    .collect(Collectors.groupingBy(
                        e -> e.getEmployeeName(),
                        Collectors.reducing(BigDecimal.ZERO, IncomeEntryResponseDTO::getAmount, BigDecimal::add)));
                grouped.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .forEach(e -> { labels.add(e.getKey()); values.add(e.getValue()); });
                break;
            }
            case "payment": {
                Map<String, BigDecimal> grouped = entries.stream()
                    .filter(e -> e.getPaymentModeDisplay() != null)
                    .collect(Collectors.groupingBy(
                        IncomeEntryResponseDTO::getPaymentModeDisplay,
                        Collectors.reducing(BigDecimal.ZERO, IncomeEntryResponseDTO::getAmount, BigDecimal::add)));
                grouped.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .forEach(e -> { labels.add(e.getKey()); values.add(e.getValue()); });
                break;
            }
        }

        return new ChartData(labels, values);
    }

    // ── Inner DTOs ───────────────────────────────────────────────────────────

    @Getter @Setter
    public static class ReportSummary {
        private BigDecimal totalIncome;
        private long       totalEntries;
        private BigDecimal avgIncome;
        private String     topCategory;

        public ReportSummary(BigDecimal totalIncome, long totalEntries,
                             BigDecimal avgIncome, String topCategory) {
            this.totalIncome  = totalIncome;
            this.totalEntries = totalEntries;
            this.avgIncome    = avgIncome;
            this.topCategory  = topCategory;
        }
    }

    @Getter @Setter
    public static class ChartData {
        private List<String>     labels;
        private List<BigDecimal> values;

        public ChartData(List<String> labels, List<BigDecimal> values) {
            this.labels = labels;
            this.values = values;
        }
    }
}
