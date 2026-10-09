package com.financialcrm.controller;

import com.financialcrm.entity.Attendance;
import com.financialcrm.entity.Employee;
import com.financialcrm.entity.EmployeeDailyWork;
import com.financialcrm.entity.EmployeeTarget;
import com.financialcrm.entity.Inquiry;
import com.financialcrm.entity.User;
import com.financialcrm.repository.EmployeeDailyWorkRepository;
import com.financialcrm.repository.EmployeeTargetRepository;
import com.financialcrm.service.*;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final CustomerService customerService;
    private final InquiryService inquiryService;
    private final FollowupService followupService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final IncomeEntryService incomeEntryService;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final EmployeeDailyWorkRepository dailyWorkRepo;
    private final EmployeeTargetRepository targetRepo;

    @GetMapping({"/", "/dashboard"})
    @Transactional(readOnly = true)
    public String dashboard(Model model) {

        // Shared by every role
        try {
            securityUtils.getCurrentUser().ifPresent(u ->
                model.addAttribute("unreadNotifications", notificationService.countUnread(u.getId()))
            );
            if (!model.containsAttribute("unreadNotifications")) {
                model.addAttribute("unreadNotifications", 0L);
            }
        } catch (Exception e) {
            log.warn("unreadNotifications error: {}", e.getMessage());
            model.addAttribute("unreadNotifications", 0L);
        }
        model.addAttribute("currentDate", LocalDate.now());

        if (securityUtils.isAdmin()) {
            return adminDashboard(model);
        } else if (securityUtils.isReceptionist()) {
            return receptionistDashboard(model);
        } else {
            return employeeDashboard(model);
        }
    }

    // ── ADMIN: full company-wide overview ───────────────────────
    private String adminDashboard(Model model) {

        try { model.addAttribute("todayVisitors", customerService.countTodayVisitors()); }
        catch (Exception e) { log.warn("todayVisitors error: {}", e.getMessage()); model.addAttribute("todayVisitors", 0L); }

        try { model.addAttribute("totalLeads", inquiryService.countTotalLeads()); }
        catch (Exception e) { log.warn("totalLeads error: {}", e.getMessage()); model.addAttribute("totalLeads", 0L); }

        try { model.addAttribute("approvedCases", inquiryService.countByStatus(Inquiry.Status.APPROVED)); }
        catch (Exception e) { log.warn("approvedCases error: {}", e.getMessage()); model.addAttribute("approvedCases", 0L); }

        try { model.addAttribute("pendingCases", inquiryService.countByStatus(Inquiry.Status.DOCUMENTS_PENDING)); }
        catch (Exception e) { log.warn("pendingCases error: {}", e.getMessage()); model.addAttribute("pendingCases", 0L); }

        try { model.addAttribute("followupToday", followupService.countTodayPending()); }
        catch (Exception e) { log.warn("followupToday error: {}", e.getMessage()); model.addAttribute("followupToday", 0L); }

        try { model.addAttribute("totalEmployees", employeeService.countActive()); }
        catch (Exception e) { log.warn("totalEmployees error: {}", e.getMessage()); model.addAttribute("totalEmployees", 0L); }

        try { model.addAttribute("presentToday", attendanceService.countByDateAndType(
                LocalDate.now(), Attendance.AttendanceType.PRESENT)); }
        catch (Exception e) { log.warn("presentToday error: {}", e.getMessage()); model.addAttribute("presentToday", 0L); }

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        try { model.addAttribute("monthlyIncome", incomeEntryService.sumByDateRange(LocalDate.now().withDayOfMonth(1), LocalDate.now())); }
        catch (Exception e) { log.warn("monthlyIncome error: {}", e.getMessage()); model.addAttribute("monthlyIncome", BigDecimal.ZERO); }

        try { model.addAttribute("monthlyExpense", expenseService.getCurrentMonthTotal()); }
        catch (Exception e) { log.warn("monthlyExpense error: {}", e.getMessage()); model.addAttribute("monthlyExpense", BigDecimal.ZERO); }

        try {
            totalIncome = incomeEntryService.sumByDateRange(LocalDate.of(2000, 1, 1), LocalDate.now());
            model.addAttribute("totalIncome", totalIncome);
        } catch (Exception e) {
            log.warn("totalIncome error: {}", e.getMessage());
            model.addAttribute("totalIncome", BigDecimal.ZERO);
        }

        try {
            totalExpense = expenseService.sumByDateRange(LocalDate.of(2000, 1, 1), LocalDate.now());
            model.addAttribute("totalExpense", totalExpense);
        } catch (Exception e) {
            log.warn("totalExpense error: {}", e.getMessage());
            model.addAttribute("totalExpense", BigDecimal.ZERO);
        }

        try {
            model.addAttribute("netProfit", totalIncome.subtract(totalExpense));
        } catch (Exception e) {
            log.warn("netProfit error: {}", e.getMessage());
            model.addAttribute("netProfit", BigDecimal.ZERO);
        }

        try {
            List<BigDecimal> incomeMonthlyTrend = new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO));
            incomeEntryService.getMonthlyTotals(Year.now().getValue()).forEach((month, value) -> {
                if (month != null && month >= 1 && month <= 12) {
                    incomeMonthlyTrend.set(month - 1, value != null ? value : BigDecimal.ZERO);
                }
            });
            model.addAttribute("incomeMonthlyTrend", incomeMonthlyTrend);
        } catch (Exception e) {
            log.warn("incomeMonthlyTrend error: {}", e.getMessage());
            model.addAttribute("incomeMonthlyTrend", Collections.nCopies(12, BigDecimal.ZERO));
        }

        try {
            List<BigDecimal> expenseMonthlyTrend = new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO));
            expenseService.getMonthlyTotals(Year.now().getValue()).forEach(row -> {
                int month = ((Number) row[0]).intValue();
                if (month >= 1 && month <= 12) {
                    expenseMonthlyTrend.set(month - 1, (BigDecimal) row[1]);
                }
            });
            model.addAttribute("expenseMonthlyTrend", expenseMonthlyTrend);
        } catch (Exception e) {
            log.warn("expenseMonthlyTrend error: {}", e.getMessage());
            model.addAttribute("expenseMonthlyTrend", Collections.nCopies(12, BigDecimal.ZERO));
        }

        try { model.addAttribute("inquiryStatusData", inquiryService.countByStatusGrouped()); }
        catch (Exception e) { log.warn("inquiryStatusData error: {}", e.getMessage()); model.addAttribute("inquiryStatusData", new ArrayList<>()); }

        try { model.addAttribute("inquiryTypeData", inquiryService.countByInquiryType()); }
        catch (Exception e) { log.warn("inquiryTypeData error: {}", e.getMessage()); model.addAttribute("inquiryTypeData", new ArrayList<>()); }

        try { model.addAttribute("monthlyLeadsData", inquiryService.countMonthlyByYear(Year.now().getValue())); }
        catch (Exception e) { log.warn("monthlyLeadsData error: {}", e.getMessage()); model.addAttribute("monthlyLeadsData", new ArrayList<>()); }

        try { model.addAttribute("monthlyIncomeData", incomeService.getMonthlyTotals(Year.now().getValue())); }
        catch (Exception e) { log.warn("monthlyIncomeData error: {}", e.getMessage()); model.addAttribute("monthlyIncomeData", new ArrayList<>()); }

        try { model.addAttribute("todayBirthdays", customerService.findTodayBirthdays()); }
        catch (Exception e) { log.warn("todayBirthdays error: {}", e.getMessage()); model.addAttribute("todayBirthdays", new ArrayList<>()); }

        try { model.addAttribute("todayFollowups", followupService.findTodayFollowups()); }
        catch (Exception e) { log.warn("todayFollowups error: {}", e.getMessage()); model.addAttribute("todayFollowups", new ArrayList<>()); }

        // ── Graph 1: Loan Income — Sanctioned vs Rejected ───────────────────
        try { model.addAttribute("loanIncomeData", inquiryService.getMonthlyLoanIncomeByStatus(Year.now().getValue())); }
        catch (Exception e) { log.warn("loanIncomeData error: {}", e.getMessage()); model.addAttribute("loanIncomeData", new ArrayList<>()); }

        // ── Graph 2: Insurance Income — Life / Health / General ─────────────
        try { model.addAttribute("insuranceIncomeData", incomeEntryService.getMonthlyInsuranceTypeTotals(Year.now().getValue())); }
        catch (Exception e) { log.warn("insuranceIncomeData error: {}", e.getMessage()); model.addAttribute("insuranceIncomeData", new ArrayList<>()); }

        // ── Graph 3: Mutual Fund Income — by category group ─────────────────
        try { model.addAttribute("mutualFundIncomeData", incomeEntryService.getMonthlyMutualFundCategoryTotals(Year.now().getValue())); }
        catch (Exception e) { log.warn("mutualFundIncomeData error: {}", e.getMessage()); model.addAttribute("mutualFundIncomeData", new ArrayList<>()); }

        return "dashboard/index";
    }

    // ── RECEPTIONIST: front-desk / operations focus, no financials ──
    private String receptionistDashboard(Model model) {

        try { model.addAttribute("todayVisitors", customerService.countTodayVisitors()); }
        catch (Exception e) { log.warn("todayVisitors error: {}", e.getMessage()); model.addAttribute("todayVisitors", 0L); }

        try { model.addAttribute("totalLeads", inquiryService.countTotalLeads()); }
        catch (Exception e) { log.warn("totalLeads error: {}", e.getMessage()); model.addAttribute("totalLeads", 0L); }

        try { model.addAttribute("approvedCases", inquiryService.countByStatus(Inquiry.Status.APPROVED)); }
        catch (Exception e) { log.warn("approvedCases error: {}", e.getMessage()); model.addAttribute("approvedCases", 0L); }

        try { model.addAttribute("pendingCases", inquiryService.countByStatus(Inquiry.Status.DOCUMENTS_PENDING)); }
        catch (Exception e) { log.warn("pendingCases error: {}", e.getMessage()); model.addAttribute("pendingCases", 0L); }

        try { model.addAttribute("followupToday", followupService.countTodayPending()); }
        catch (Exception e) { log.warn("followupToday error: {}", e.getMessage()); model.addAttribute("followupToday", 0L); }

        try { model.addAttribute("inquiryStatusData", inquiryService.countByStatusGrouped()); }
        catch (Exception e) { log.warn("inquiryStatusData error: {}", e.getMessage()); model.addAttribute("inquiryStatusData", new ArrayList<>()); }

        try { model.addAttribute("monthlyLeadsData", inquiryService.countMonthlyByYear(Year.now().getValue())); }
        catch (Exception e) { log.warn("monthlyLeadsData error: {}", e.getMessage()); model.addAttribute("monthlyLeadsData", new ArrayList<>()); }

        try { model.addAttribute("todayBirthdays", customerService.findTodayBirthdays()); }
        catch (Exception e) { log.warn("todayBirthdays error: {}", e.getMessage()); model.addAttribute("todayBirthdays", new ArrayList<>()); }

        try { model.addAttribute("todayFollowups", followupService.findTodayFollowups()); }
        catch (Exception e) { log.warn("todayFollowups error: {}", e.getMessage()); model.addAttribute("todayFollowups", new ArrayList<>()); }

        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            Employee linkedEmployee = employeeService.findByUserId(currentUser.getId()).orElse(null);
            model.addAttribute("linkedEmployee", linkedEmployee);
            if (linkedEmployee != null) {
                LocalDate today = LocalDate.now();
                List<EmployeeDailyWork> assignedWork = dailyWorkRepo.findByEmployeeAndDateRange(
                        linkedEmployee.getId(), today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()), false);
                model.addAttribute("assignedWork", assignedWork);
                model.addAttribute("assignedWorkToday", assignedWork.stream()
                        .filter(work -> work.getWorkDate() != null && work.getWorkDate().equals(today))
                        .toList());
            } else {
                model.addAttribute("assignedWork", new ArrayList<EmployeeDailyWork>());
                model.addAttribute("assignedWorkToday", new ArrayList<EmployeeDailyWork>());
            }
        } catch (Exception e) {
            log.warn("assignedWork error: {}", e.getMessage());
            model.addAttribute("linkedEmployee", null);
            model.addAttribute("assignedWork", new ArrayList<EmployeeDailyWork>());
            model.addAttribute("assignedWorkToday", new ArrayList<EmployeeDailyWork>());
        }

        return "dashboard/receptionist";
    }

    // ── EMPLOYEE: personal work summary only, no company financials ──
    private String employeeDashboard(Model model) {

        User currentUser = securityUtils.getCurrentUserOrThrow();
        Employee employee = employeeService.findAllActive().stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(currentUser.getId()))
                .findFirst().orElse(null);

        model.addAttribute("employee", employee);

        if (employee != null) {
            LocalDate today = LocalDate.now();

            try {
                Object[] totals = dailyWorkRepo.getMonthlyTotals(employee.getId(), today.getMonthValue(), today.getYear());
                model.addAttribute("monthTotals", unwrapTotals(totals));
            } catch (Exception e) {
                log.warn("monthTotals error: {}", e.getMessage());
                model.addAttribute("monthTotals", new Object[]{0, 0, 0});
            }

            try {
                EmployeeTarget target = targetRepo.findByEmployeeIdAndTargetMonthAndTargetYear(
                        employee.getId(), today.getMonthValue(), today.getYear()).orElse(null);
                model.addAttribute("target", target);
            } catch (Exception e) {
                log.warn("target error: {}", e.getMessage());
                model.addAttribute("target", null);
            }

            try {
                model.addAttribute("todayAttendance",
                        attendanceService.findByEmployeeAndDate(employee.getId(), today).orElse(null));
            } catch (Exception e) {
                log.warn("todayAttendance error: {}", e.getMessage());
                model.addAttribute("todayAttendance", null);
            }

            try {
                List<EmployeeDailyWork> assignedWork = dailyWorkRepo.findByEmployeeAndDateRange(
                        employee.getId(), today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()), false);
                model.addAttribute("assignedWork", assignedWork);
                model.addAttribute("assignedWorkToday", assignedWork.stream()
                        .filter(work -> work.getWorkDate() != null && work.getWorkDate().equals(today))
                        .toList());
            } catch (Exception e) {
                log.warn("assignedWork error: {}", e.getMessage());
                model.addAttribute("assignedWork", new ArrayList<EmployeeDailyWork>());
                model.addAttribute("assignedWorkToday", new ArrayList<EmployeeDailyWork>());
            }
        } else {
            model.addAttribute("monthTotals", new Object[]{0, 0, 0});
            model.addAttribute("target", null);
            model.addAttribute("todayAttendance", null);
            model.addAttribute("assignedWork", new ArrayList<EmployeeDailyWork>());
            model.addAttribute("assignedWorkToday", new ArrayList<EmployeeDailyWork>());
        }

        try { model.addAttribute("todayBirthdays", customerService.findTodayBirthdays()); }
        catch (Exception e) { log.warn("todayBirthdays error: {}", e.getMessage()); model.addAttribute("todayBirthdays", new ArrayList<>()); }

        return "dashboard/employee";
    }

    private Object[] unwrapTotals(Object[] raw) {
        if (raw == null || raw.length == 0) return new Object[]{0, 0, 0};
        if (raw[0] instanceof Object[]) {
            raw = (Object[]) raw[0];
        }
        Object[] result = new Object[]{0, 0, 0};
        for (int i = 0; i < Math.min(raw.length, 3); i++) {
            result[i] = raw[i] != null ? raw[i] : 0;
        }
        return result;
    }
}
