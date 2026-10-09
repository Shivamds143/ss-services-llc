package com.financialcrm.controller;

import com.financialcrm.entity.Employee;
import com.financialcrm.repository.EmployeeRepository;
import com.financialcrm.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/reports")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
public class ReportController {

    private final ReportService reportService;
    private final EmployeeRepository employeeRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @GetMapping
    public String reportsHome(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("today", today);
        model.addAttribute("firstOfMonth", today.withDayOfMonth(1));
        model.addAttribute("firstOfYear", today.withDayOfYear(1));

        // For employee dropdown in attendance / performance reports
        List<Employee> employees = employeeRepository.findByIsActiveTrueOrderByEmployeeNameAsc();
        model.addAttribute("employees", employees);

        return "reports/index";
    }

    // ─── 1. DAILY REPORT ────────────────────────────────────────
    @GetMapping("/daily/excel")
    public ResponseEntity<byte[]> dailyReportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        try {
            byte[] data = reportService.generateDailyReportExcel(reportDate);
            return buildExcelResponse(data, "Daily_Report_" + reportDate.format(FMT) + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating daily report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── 2. WEEKLY REPORT ───────────────────────────────────────
    @GetMapping("/weekly/excel")
    public ResponseEntity<byte[]> weeklyReportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] data = reportService.generateWeeklyReportExcel(startDate, endDate);
            return buildExcelResponse(data, "Weekly_Report_" + startDate.format(FMT) + "_" + endDate.format(FMT) + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating weekly report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── 3. MONTHLY SALES REPORT ────────────────────────────────
    @GetMapping("/monthly-sales/excel")
    public ResponseEntity<byte[]> monthlySalesExcel(
            @RequestParam int month,
            @RequestParam int year) {
        try {
            byte[] data = reportService.generateMonthlySalesExcel(month, year);
            String monthName = LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM_yyyy"));
            return buildExcelResponse(data, "Monthly_Sales_" + monthName + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating monthly sales report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── 4. QTY SALES REPORT ────────────────────────────────────
    @GetMapping("/qty-sales/excel")
    public ResponseEntity<byte[]> qtySalesExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] data = reportService.generateQtySalesExcel(startDate, endDate);
            return buildExcelResponse(data, "Qty_Sales_" + startDate.format(FMT) + "_" + endDate.format(FMT) + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating qty sales report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── 5. YEAR SALES REPORT ───────────────────────────────────
    @GetMapping("/year-sales/excel")
    public ResponseEntity<byte[]> yearSalesExcel(
            @RequestParam int year) {
        try {
            byte[] data = reportService.generateYearSalesExcel(year);
            return buildExcelResponse(data, "Year_Sales_" + year + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating year sales report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── 6. EMPLOYEE PERFORMANCE REPORT ─────────────────────────
    @GetMapping("/employee-performance/excel")
    public ResponseEntity<byte[]> employeePerformanceExcel(
            @RequestParam int month,
            @RequestParam int year) {
        try {
            byte[] data = reportService.generateEmployeePerformanceExcel(month, year);
            String monthName = LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM_yyyy"));
            return buildExcelResponse(data, "Employee_Performance_" + monthName + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating employee performance report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── 7. LOAN DISBURSEMENT REPORT ────────────────────────────
    @GetMapping("/loan-disbursement/excel")
    public ResponseEntity<byte[]> loanDisbursementExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] data = reportService.generateLoanDisbursementExcel(startDate, endDate);
            return buildExcelResponse(data, "Loan_Disbursement_" + startDate.format(FMT) + "_" + endDate.format(FMT) + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating loan disbursement report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── 8. INSURANCE RENEWAL REPORT ────────────────────────────
    @GetMapping("/insurance-renewal/excel")
    public ResponseEntity<byte[]> insuranceRenewalExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] data = reportService.generateInsuranceRenewalExcel(startDate, endDate);
            return buildExcelResponse(data, "Insurance_Renewal_" + startDate.format(FMT) + "_" + endDate.format(FMT) + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating insurance renewal report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── EXISTING REPORTS (kept intact) ─────────────────────────
    @GetMapping("/inquiries/excel")
    public ResponseEntity<byte[]> inquiryExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] data = reportService.generateInquiryReportExcel(startDate, endDate);
            return buildExcelResponse(data, "Inquiry_Report_" + startDate.format(FMT) + "_" + endDate.format(FMT) + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating inquiry report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/expenses/excel")
    public ResponseEntity<byte[]> expenseExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] data = reportService.generateExpenseReportExcel(startDate, endDate);
            return buildExcelResponse(data, "Expense_Report_" + startDate.format(FMT) + "_" + endDate.format(FMT) + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating expense report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/income/excel")
    public ResponseEntity<byte[]> incomeExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] data = reportService.generateIncomeReportExcel(startDate, endDate);
            return buildExcelResponse(data, "Income_Report_" + startDate.format(FMT) + "_" + endDate.format(FMT) + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating income report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/attendance/excel")
    public ResponseEntity<byte[]> attendanceExcel(
            @RequestParam Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        try {
            byte[] data = reportService.generateAttendanceReportExcel(employeeId, month, year);
            return buildExcelResponse(data, "Attendance_" + month + "_" + year + ".xlsx");
        } catch (Exception e) {
            log.error("Error generating attendance report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── HELPER ─────────────────────────────────────────────────
    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}