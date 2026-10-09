package com.financialcrm.service;

import com.financialcrm.entity.*;
import com.financialcrm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final InquiryRepository inquiryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CustomerRepository customerRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final FollowupRepository followupRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ════════════════════════════════════════════════════════════
    // 1. DAILY REPORT
    // ════════════════════════════════════════════════════════════
    public byte[] generateDailyReportExcel(LocalDate reportDate) throws IOException {
        List<Inquiry> inquiries = inquiryRepository.findByDateRange(reportDate, reportDate);
        List<Income>  incomes   = incomeRepository.findByDateRange(reportDate, reportDate);
        List<Expense> expenses  = expenseRepository.findByDateRange(reportDate, reportDate);
        List<Followup> followups = followupRepository.findFollowupsByDateRange(reportDate, reportDate);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── Sheet 1: Summary ────────────────────────────────
            XSSFSheet summary = wb.createSheet("Daily Summary");
            CellStyle titleStyle   = createTitleStyle(wb);
            CellStyle headerStyle  = createHeaderStyle(wb);
            CellStyle dataStyle    = createDataStyle(wb);
            CellStyle currencyStyle= createCurrencyStyle(wb);
            CellStyle sectionStyle = createSectionStyle(wb);

            Row titleRow = summary.createRow(0);
            Cell t = titleRow.createCell(0);
            t.setCellValue("Daily Business Report — " + reportDate.format(DATE_FMT));
            t.setCellStyle(titleStyle);
            summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            // Summary KPIs
            int r = 2;
            writeKpiRow(summary, r++, "New Inquiries Today",       String.valueOf(inquiries.size()), sectionStyle, dataStyle);
            BigDecimal totalIncome = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpense= expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            writeKpiRow(summary, r++, "Total Income Today",        "₹ " + totalIncome.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Total Expenses Today",      "₹ " + totalExpense.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Net (Income - Expense)",    "₹ " + totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Follow-ups Scheduled",      String.valueOf(followups.size()), sectionStyle, dataStyle);
            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            // ── Sheet 2: Inquiries ───────────────────────────────
            buildInquirySheet(wb, inquiries, "Inquiries (" + reportDate.format(DATE_FMT) + ")", headerStyle, dataStyle, currencyStyle, titleStyle);

            // ── Sheet 3: Income ──────────────────────────────────
            buildIncomeSheet(wb, incomes, "Income (" + reportDate.format(DATE_FMT) + ")", headerStyle, dataStyle, currencyStyle, titleStyle);

            // ── Sheet 4: Expenses ────────────────────────────────
            buildExpenseSheet(wb, expenses, "Expenses (" + reportDate.format(DATE_FMT) + ")", headerStyle, dataStyle, currencyStyle, titleStyle);

            // ── Sheet 5: Follow-ups ──────────────────────────────
            buildFollowupSheet(wb, followups, "Follow-ups (" + reportDate.format(DATE_FMT) + ")", headerStyle, dataStyle, titleStyle);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // 2. WEEKLY REPORT
    // ════════════════════════════════════════════════════════════
    public byte[] generateWeeklyReportExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Inquiry> inquiries = inquiryRepository.findByDateRange(startDate, endDate);
        List<Income>  incomes   = incomeRepository.findByDateRange(startDate, endDate);
        List<Expense> expenses  = expenseRepository.findByDateRange(startDate, endDate);
        List<Followup> followups = followupRepository.findFollowupsByDateRange(startDate, endDate);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle    = createTitleStyle(wb);
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle sectionStyle  = createSectionStyle(wb);

            // ── Sheet 1: Weekly Summary ──────────────────────────
            XSSFSheet summary = wb.createSheet("Weekly Summary");
            Row titleRow = summary.createRow(0);
            Cell t = titleRow.createCell(0);
            t.setCellValue("Weekly Report: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT));
            t.setCellStyle(titleStyle);
            summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            int r = 2;
            BigDecimal totalIncome  = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            writeKpiRow(summary, r++, "Total Inquiries",  String.valueOf(inquiries.size()), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Approved Inquiries",
                String.valueOf(inquiries.stream().filter(i -> i.getStatus() == Inquiry.Status.APPROVED).count()), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Rejected Inquiries",
                String.valueOf(inquiries.stream().filter(i -> i.getStatus() == Inquiry.Status.REJECTED).count()), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Total Income",     "₹ " + totalIncome.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Total Expenses",   "₹ " + totalExpense.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Net Profit/Loss",  "₹ " + totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Follow-ups Done",  String.valueOf(followups.size()), sectionStyle, dataStyle);
            summary.autoSizeColumn(0); summary.autoSizeColumn(1);

            buildInquirySheet(wb, inquiries, "Inquiries", headerStyle, dataStyle, currencyStyle, titleStyle);
            buildIncomeSheet(wb, incomes, "Income", headerStyle, dataStyle, currencyStyle, titleStyle);
            buildExpenseSheet(wb, expenses, "Expenses", headerStyle, dataStyle, currencyStyle, titleStyle);
            buildFollowupSheet(wb, followups, "Follow-ups", headerStyle, dataStyle, titleStyle);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // 3. MONTHLY SALES REPORT
    // ════════════════════════════════════════════════════════════
    public byte[] generateMonthlySalesExcel(int month, int year) throws IOException {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        String monthLabel = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;

        List<Inquiry> inquiries = inquiryRepository.findByDateRange(start, end);
        List<Income>  incomes   = incomeRepository.findByDateRange(start, end);
        List<Expense> expenses  = expenseRepository.findByDateRange(start, end);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle    = createTitleStyle(wb);
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle sectionStyle  = createSectionStyle(wb);

            // ── Sheet 1: Monthly Summary ─────────────────────────
            XSSFSheet summary = wb.createSheet("Monthly Summary");
            Row titleRow = summary.createRow(0);
            Cell t = titleRow.createCell(0);
            t.setCellValue("Monthly Sales Report — " + monthLabel);
            t.setCellStyle(titleStyle);
            summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            int r = 2;
            BigDecimal totalIncome  = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            long approvedCount = inquiries.stream().filter(i -> i.getStatus() == Inquiry.Status.APPROVED).count();

            writeKpiRow(summary, r++, "Total Leads",         String.valueOf(inquiries.size()), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Loans Approved",      String.valueOf(approvedCount), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Conversion Rate",
                approvedCount > 0 && inquiries.size() > 0
                    ? String.format("%.1f%%", (approvedCount * 100.0) / inquiries.size())
                    : "0%", sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Total Sales Income",  "₹ " + totalIncome.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Total Expenses",      "₹ " + totalExpense.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Net Profit",          "₹ " + totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            summary.autoSizeColumn(0); summary.autoSizeColumn(1);

            // ── Sheet 2: Category-wise Income ───────────────────
            XSSFSheet catSheet = wb.createSheet("Category Wise Income");
            Row catTitle = catSheet.createRow(0);
            catTitle.createCell(0).setCellValue("Income by Category — " + monthLabel);
            catTitle.getCell(0).setCellStyle(titleStyle);
            catSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            Row catHeader = catSheet.createRow(2);
            String[] catHeaders = {"#", "Category", "Amount (₹)"};
            for (int i = 0; i < catHeaders.length; i++) {
                Cell c = catHeader.createCell(i); c.setCellValue(catHeaders[i]); c.setCellStyle(headerStyle);
            }
            Map<String, BigDecimal> catMap = incomes.stream().collect(
                Collectors.groupingBy(inc -> inc.getCategory().getCategoryName(),
                    Collectors.reducing(BigDecimal.ZERO, Income::getAmount, BigDecimal::add)));
            int catRow = 3;
            for (Map.Entry<String, BigDecimal> entry : catMap.entrySet()) {
                Row row = catSheet.createRow(catRow++);
                row.createCell(0).setCellValue(catRow - 3);
                row.createCell(1).setCellValue(entry.getKey());
                Cell amtC = row.createCell(2); amtC.setCellValue(entry.getValue().doubleValue()); amtC.setCellStyle(currencyStyle);
                for (int i = 0; i < 3; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(2).setCellStyle(currencyStyle);
            }
            for (int i = 0; i < 3; i++) catSheet.autoSizeColumn(i);

            buildInquirySheet(wb, inquiries, "All Inquiries", headerStyle, dataStyle, currencyStyle, titleStyle);
            buildIncomeSheet(wb, incomes, "All Income", headerStyle, dataStyle, currencyStyle, titleStyle);
            buildExpenseSheet(wb, expenses, "All Expenses", headerStyle, dataStyle, currencyStyle, titleStyle);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // 4. QTY SALES REPORT  (Inquiry-type wise counts & amounts)
    // ════════════════════════════════════════════════════════════
    public byte[] generateQtySalesExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Inquiry> inquiries = inquiryRepository.findByDateRange(startDate, endDate);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle    = createTitleStyle(wb);
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            // ── Sheet 1: Qty Summary by Type ─────────────────────
            XSSFSheet sheet = wb.createSheet("Qty Sales Summary");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Quantity Sales Report: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT));
            title.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            String[] headers = {"#", "Inquiry Type", "Total Count", "Approved", "Rejected", "Pending", "Total Loan Amt (₹)"};
            Row hRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle);
            }

            // Group by inquiry type
            Map<String, List<Inquiry>> byType = inquiries.stream()
                .collect(Collectors.groupingBy(i -> i.getInquiryType().getTypeName()));

            int r = 3;
            int serial = 1;
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (Map.Entry<String, List<Inquiry>> entry : byType.entrySet()) {
                List<Inquiry> list = entry.getValue();
                long approved  = list.stream().filter(i -> i.getStatus() == Inquiry.Status.APPROVED).count();
                long rejected  = list.stream().filter(i -> i.getStatus() == Inquiry.Status.REJECTED).count();
                long pending   = list.stream().filter(i -> i.getStatus() == Inquiry.Status.NEW
                                                        || i.getStatus() == Inquiry.Status.FOLLOW_UP
                                                        || i.getStatus() == Inquiry.Status.DOCUMENTS_PENDING).count();
                BigDecimal totalLoan = list.stream()
                    .filter(i -> i.getLoanAmount() != null)
                    .map(Inquiry::getLoanAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                grandTotal = grandTotal.add(totalLoan);

                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(serial++);
                row.createCell(1).setCellValue(entry.getKey());
                row.createCell(2).setCellValue(list.size());
                row.createCell(3).setCellValue(approved);
                row.createCell(4).setCellValue(rejected);
                row.createCell(5).setCellValue(pending);
                Cell loanCell = row.createCell(6); loanCell.setCellValue(totalLoan.doubleValue()); loanCell.setCellStyle(currencyStyle);
                for (int i = 0; i < 7; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(6).setCellStyle(currencyStyle);
            }

            // Grand total row
            Row totalRow = sheet.createRow(r + 1);
            Cell tl = totalRow.createCell(1); tl.setCellValue("GRAND TOTAL"); tl.setCellStyle(headerStyle);
            Cell tc = totalRow.createCell(2); tc.setCellValue(inquiries.size()); tc.setCellStyle(headerStyle);
            Cell ta = totalRow.createCell(6); ta.setCellValue(grandTotal.doubleValue()); ta.setCellStyle(currencyStyle);

            for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);

            // ── Sheet 2: Detailed List ───────────────────────────
            buildInquirySheet(wb, inquiries, "Detailed Inquiries", headerStyle, dataStyle, currencyStyle, titleStyle);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // 5. YEAR SALES REPORT
    // ════════════════════════════════════════════════════════════
    public byte[] generateYearSalesExcel(int year) throws IOException {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);
        List<Inquiry> inquiries = inquiryRepository.findByDateRange(start, end);
        List<Income>  incomes   = incomeRepository.findByDateRange(start, end);
        List<Expense> expenses  = expenseRepository.findByDateRange(start, end);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle    = createTitleStyle(wb);
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle sectionStyle  = createSectionStyle(wb);

            // ── Sheet 1: Year Summary ────────────────────────────
            XSSFSheet summary = wb.createSheet("Year Summary");
            Row title = summary.createRow(0);
            title.createCell(0).setCellValue("Annual Sales Report — Year " + year);
            title.getCell(0).setCellStyle(titleStyle);
            summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            int r = 2;
            BigDecimal totalIncome  = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            long approvedCount = inquiries.stream().filter(i -> i.getStatus() == Inquiry.Status.APPROVED).count();

            writeKpiRow(summary, r++, "Total Inquiries",    String.valueOf(inquiries.size()), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Loans Approved",     String.valueOf(approvedCount), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Total Revenue",      "₹ " + totalIncome.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Total Expenses",     "₹ " + totalExpense.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Net Profit/Loss",    "₹ " + totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            summary.autoSizeColumn(0); summary.autoSizeColumn(1);

            // ── Sheet 2: Month-wise Breakdown ────────────────────
            XSSFSheet monthSheet = wb.createSheet("Month-wise Breakdown");
            Row monthTitle = monthSheet.createRow(0);
            monthTitle.createCell(0).setCellValue("Month-wise Sales Breakdown — " + year);
            monthTitle.getCell(0).setCellStyle(titleStyle);
            monthSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            String[] mHeaders = {"Month", "Inquiries", "Approved", "Income (₹)", "Expenses (₹)", "Net (₹)"};
            Row mHeaderRow = monthSheet.createRow(2);
            for (int i = 0; i < mHeaders.length; i++) {
                Cell c = mHeaderRow.createCell(i); c.setCellValue(mHeaders[i]); c.setCellStyle(headerStyle);
            }

            int mr = 3;
            for (int m = 1; m <= 12; m++) {
                LocalDate ms = LocalDate.of(year, m, 1);
                LocalDate me = ms.withDayOfMonth(ms.lengthOfMonth());
                final int finalM = m;

                long inqCount = inquiries.stream().filter(i -> i.getInquiryDate().getMonthValue() == finalM).count();
                long appCount = inquiries.stream().filter(i -> i.getInquiryDate().getMonthValue() == finalM
                                                             && i.getStatus() == Inquiry.Status.APPROVED).count();
                BigDecimal mIncome  = incomes.stream().filter(i -> i.getIncomeDate().getMonthValue() == finalM).map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal mExpense = expenses.stream().filter(e -> e.getExpenseDate().getMonthValue() == finalM).map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

                Row row = monthSheet.createRow(mr++);
                row.createCell(0).setCellValue(Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                row.createCell(1).setCellValue(inqCount);
                row.createCell(2).setCellValue(appCount);
                Cell incC = row.createCell(3); incC.setCellValue(mIncome.doubleValue());  incC.setCellStyle(currencyStyle);
                Cell expC = row.createCell(4); expC.setCellValue(mExpense.doubleValue()); expC.setCellStyle(currencyStyle);
                Cell netC = row.createCell(5); netC.setCellValue(mIncome.subtract(mExpense).doubleValue()); netC.setCellStyle(currencyStyle);
                for (int i = 0; i < 6; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(3).setCellStyle(currencyStyle);
                row.getCell(4).setCellStyle(currencyStyle);
                row.getCell(5).setCellStyle(currencyStyle);
            }
            for (int i = 0; i < 6; i++) monthSheet.autoSizeColumn(i);

            buildInquirySheet(wb, inquiries, "All Inquiries", headerStyle, dataStyle, currencyStyle, titleStyle);
            buildIncomeSheet(wb, incomes, "All Income", headerStyle, dataStyle, currencyStyle, titleStyle);
            buildExpenseSheet(wb, expenses, "All Expenses", headerStyle, dataStyle, currencyStyle, titleStyle);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // 6. EMPLOYEE PERFORMANCE REPORT
    // ════════════════════════════════════════════════════════════
    public byte[] generateEmployeePerformanceExcel(int month, int year) throws IOException {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        String monthLabel = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;

        List<Employee> employees = employeeRepository.findByIsActiveTrueOrderByEmployeeNameAsc();
        List<Inquiry>  allInquiries = inquiryRepository.findByDateRange(start, end);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle    = createTitleStyle(wb);
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            // ── Sheet 1: Employee Summary ────────────────────────
            XSSFSheet sheet = wb.createSheet("Employee Performance");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Employee Performance Report — " + monthLabel);
            title.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            String[] headers = {"#", "Employee ID", "Employee Name", "Designation", "Assigned Leads",
                                 "Approved", "Rejected", "Pending", "Conversion %", "Present Days"};
            Row hRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle);
            }

            int r = 3;
            for (Employee emp : employees) {
                List<Inquiry> empInq = allInquiries.stream()
                    .filter(i -> i.getAssignedTo() != null
                              && emp.getUser() != null
                              && i.getAssignedTo().getId().equals(emp.getUser().getId()))
                    .collect(Collectors.toList());

                long approved  = empInq.stream().filter(i -> i.getStatus() == Inquiry.Status.APPROVED).count();
                long rejected  = empInq.stream().filter(i -> i.getStatus() == Inquiry.Status.REJECTED).count();
                long pending   = empInq.size() - approved - rejected;
                double convRate= empInq.isEmpty() ? 0 : (approved * 100.0) / empInq.size();

                // Attendance: count PRESENT days
                List<Object[]> attSummary = attendanceRepository.getMonthlyAttendanceSummary(emp.getId(), month, year);
                long presentDays = attSummary.stream()
                    .filter(a -> a[0].toString().equals("PRESENT"))
                    .mapToLong(a -> ((Number) a[1]).longValue())
                    .sum();

                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(r - 3);
                row.createCell(1).setCellValue(emp.getEmployeeId());
                row.createCell(2).setCellValue(emp.getEmployeeName());
                row.createCell(3).setCellValue(emp.getDesignation() != null ? emp.getDesignation() : "");
                row.createCell(4).setCellValue(empInq.size());
                row.createCell(5).setCellValue(approved);
                row.createCell(6).setCellValue(rejected);
                row.createCell(7).setCellValue(pending);
                row.createCell(8).setCellValue(String.format("%.1f%%", convRate));
                row.createCell(9).setCellValue(presentDays);
                for (int i = 0; i < 10; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // 7. LOAN DISBURSEMENT REPORT
    // ════════════════════════════════════════════════════════════
    public byte[] generateLoanDisbursementExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        // Get all APPROVED inquiries in date range
        List<Inquiry> approvedInquiries = inquiryRepository.findByDateRange(startDate, endDate)
            .stream()
            .filter(i -> i.getStatus() == Inquiry.Status.APPROVED)
            .collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle    = createTitleStyle(wb);
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle sectionStyle  = createSectionStyle(wb);

            // ── Sheet 1: Summary ─────────────────────────────────
            XSSFSheet summary = wb.createSheet("Disbursement Summary");
            Row title = summary.createRow(0);
            title.createCell(0).setCellValue("Loan Disbursement Report: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT));
            title.getCell(0).setCellStyle(titleStyle);
            summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            BigDecimal totalDisbursed = approvedInquiries.stream()
                .filter(i -> i.getLoanAmount() != null)
                .map(Inquiry::getLoanAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            int r = 2;
            writeKpiRow(summary, r++, "Total Loans Disbursed",   String.valueOf(approvedInquiries.size()), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Total Amount Disbursed",  "₹ " + totalDisbursed.setScale(2, RoundingMode.HALF_UP), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Average Loan Amount",
                approvedInquiries.isEmpty() ? "₹ 0"
                    : "₹ " + totalDisbursed.divide(BigDecimal.valueOf(approvedInquiries.size()), 2, RoundingMode.HALF_UP),
                sectionStyle, dataStyle);
            summary.autoSizeColumn(0); summary.autoSizeColumn(1);

            // ── Sheet 2: Bank-wise Disbursement ──────────────────
            XSSFSheet bankSheet = wb.createSheet("Bank-wise Summary");
            Row bankTitle = bankSheet.createRow(0);
            bankTitle.createCell(0).setCellValue("Bank-wise Loan Disbursement");
            bankTitle.getCell(0).setCellStyle(titleStyle);
            bankSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            Row bankHeader = bankSheet.createRow(2);
            String[] bankHeaders = {"#", "Bank Name", "No. of Loans", "Total Amount (₹)"};
            for (int i = 0; i < bankHeaders.length; i++) {
                Cell c = bankHeader.createCell(i); c.setCellValue(bankHeaders[i]); c.setCellStyle(headerStyle);
            }

            Map<String, List<Inquiry>> byBank = approvedInquiries.stream()
                .collect(Collectors.groupingBy(i -> i.getBankName() != null ? i.getBankName() : "Unknown"));

            int br = 3;
            for (Map.Entry<String, List<Inquiry>> entry : byBank.entrySet()) {
                BigDecimal bankTotal = entry.getValue().stream()
                    .filter(i -> i.getLoanAmount() != null)
                    .map(Inquiry::getLoanAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                Row row = bankSheet.createRow(br++);
                row.createCell(0).setCellValue(br - 3);
                row.createCell(1).setCellValue(entry.getKey());
                row.createCell(2).setCellValue(entry.getValue().size());
                Cell amtC = row.createCell(3); amtC.setCellValue(bankTotal.doubleValue()); amtC.setCellStyle(currencyStyle);
                for (int i = 0; i < 4; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(3).setCellStyle(currencyStyle);
            }
            for (int i = 0; i < 4; i++) bankSheet.autoSizeColumn(i);

            // ── Sheet 3: Detailed Disbursement List ──────────────
            XSSFSheet detail = wb.createSheet("Loan Details");
            Row detailTitle = detail.createRow(0);
            detailTitle.createCell(0).setCellValue("Loan Disbursement Detail");
            detailTitle.getCell(0).setCellStyle(titleStyle);
            detail.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            String[] dHeaders = {"#", "Inquiry Date", "Close Date", "Customer Name", "Mobile",
                                  "Loan Type", "Bank", "Loan Amount (₹)", "Interest Rate", "Tenure (Months)", "EMI (₹)", "Assigned To"};
            Row dHeaderRow = detail.createRow(2);
            for (int i = 0; i < dHeaders.length; i++) {
                Cell c = dHeaderRow.createCell(i); c.setCellValue(dHeaders[i]); c.setCellStyle(headerStyle);
            }

            int dr = 3;
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (Inquiry inq : approvedInquiries) {
                Row row = detail.createRow(dr++);
                row.createCell(0).setCellValue(dr - 3);
                row.createCell(1).setCellValue(inq.getInquiryDate().format(DATE_FMT));
                row.createCell(2).setCellValue(inq.getActualCloseDate() != null ? inq.getActualCloseDate().format(DATE_FMT) : "");
                row.createCell(3).setCellValue(inq.getCustomer().getCustomerName());
                row.createCell(4).setCellValue(inq.getCustomer().getMobileNumber());
                row.createCell(5).setCellValue(inq.getInquiryType().getTypeName());
                row.createCell(6).setCellValue(inq.getBankName() != null ? inq.getBankName() : "");
                BigDecimal loanAmt = inq.getLoanAmount() != null ? inq.getLoanAmount() : BigDecimal.ZERO;
                Cell loanC = row.createCell(7); loanC.setCellValue(loanAmt.doubleValue()); loanC.setCellStyle(currencyStyle);
                row.createCell(8).setCellValue(inq.getInterestRate() != null ? inq.getInterestRate().toString() + "%" : "");
                row.createCell(9).setCellValue(inq.getTenureMonths() != null ? inq.getTenureMonths() : 0);
                Cell emiC = row.createCell(10);
                emiC.setCellValue(inq.getEmiAmount() != null ? inq.getEmiAmount().doubleValue() : 0);
                emiC.setCellStyle(currencyStyle);
                row.createCell(11).setCellValue(inq.getAssignedTo() != null ? inq.getAssignedTo().getFullName() : "");
                grandTotal = grandTotal.add(loanAmt);
                for (int i = 0; i < 12; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(7).setCellStyle(currencyStyle);
                row.getCell(10).setCellStyle(currencyStyle);
            }

            // Total row
            Row grandRow = detail.createRow(dr + 1);
            Cell gl = grandRow.createCell(3); gl.setCellValue("TOTAL"); gl.setCellStyle(headerStyle);
            Cell ga = grandRow.createCell(7); ga.setCellValue(grandTotal.doubleValue()); ga.setCellStyle(currencyStyle);

            for (int i = 0; i < dHeaders.length; i++) detail.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // 8. INSURANCE RENEWAL REPORT
    // ════════════════════════════════════════════════════════════
    public byte[] generateInsuranceRenewalExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        // Get Insurance-type inquiries (type name contains "Insurance" or "insurance")
        List<Inquiry> allInquiries = inquiryRepository.findByDateRange(startDate, endDate);
        List<Inquiry> insuranceInq = allInquiries.stream()
            .filter(i -> i.getInquiryType().getTypeName().toLowerCase().contains("insurance"))
            .collect(Collectors.toList());

        // Also include inquiries where expected_close_date is within the range (renewals due)
        List<Inquiry> renewalsDue = inquiryRepository.findByDateRange(
                LocalDate.now(), LocalDate.now().plusDays(30))
            .stream()
            .filter(i -> i.getInquiryType().getTypeName().toLowerCase().contains("insurance"))
            .filter(i -> i.getStatus() != Inquiry.Status.CLOSED && i.getStatus() != Inquiry.Status.REJECTED)
            .collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle    = createTitleStyle(wb);
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle sectionStyle  = createSectionStyle(wb);
            CellStyle alertStyle    = createAlertStyle(wb);

            // ── Sheet 1: Summary ─────────────────────────────────
            XSSFSheet summary = wb.createSheet("Renewal Summary");
            Row title = summary.createRow(0);
            title.createCell(0).setCellValue("Insurance Renewal Report: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT));
            title.getCell(0).setCellStyle(titleStyle);
            summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            int r = 2;
            writeKpiRow(summary, r++, "Total Insurance Records",  String.valueOf(insuranceInq.size()), sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Active Policies",
                String.valueOf(insuranceInq.stream().filter(i -> i.getStatus() != Inquiry.Status.CLOSED && i.getStatus() != Inquiry.Status.REJECTED).count()),
                sectionStyle, dataStyle);
            writeKpiRow(summary, r++, "Renewals Due (Next 30 Days)", String.valueOf(renewalsDue.size()), sectionStyle, dataStyle);
            summary.autoSizeColumn(0); summary.autoSizeColumn(1);

            // ── Sheet 2: Renewals Due ────────────────────────────
            XSSFSheet dueSheet = wb.createSheet("Renewals Due (30 Days)");
            Row dueTitle = dueSheet.createRow(0);
            dueTitle.createCell(0).setCellValue("⚠ Insurance Renewals Due in Next 30 Days");
            dueTitle.getCell(0).setCellStyle(titleStyle);
            dueSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            String[] dueHeaders = {"#", "Customer Name", "Mobile", "Insurance Type", "Bank/Company",
                                    "Policy Amount (₹)", "Expected Renewal Date", "Assigned To", "Status"};
            Row dueHeaderRow = dueSheet.createRow(2);
            for (int i = 0; i < dueHeaders.length; i++) {
                Cell c = dueHeaderRow.createCell(i); c.setCellValue(dueHeaders[i]); c.setCellStyle(headerStyle);
            }

            int dr = 3;
            for (Inquiry inq : renewalsDue) {
                Row row = dueSheet.createRow(dr++);
                row.createCell(0).setCellValue(dr - 3);
                row.createCell(1).setCellValue(inq.getCustomer().getCustomerName());
                row.createCell(2).setCellValue(inq.getCustomer().getMobileNumber());
                row.createCell(3).setCellValue(inq.getInquiryType().getTypeName());
                row.createCell(4).setCellValue(inq.getBankName() != null ? inq.getBankName() : "");
                Cell amtC = row.createCell(5);
                amtC.setCellValue(inq.getLoanAmount() != null ? inq.getLoanAmount().doubleValue() : 0);
                amtC.setCellStyle(currencyStyle);
                row.createCell(6).setCellValue(inq.getExpectedCloseDate() != null ? inq.getExpectedCloseDate().format(DATE_FMT) : "");
                row.createCell(7).setCellValue(inq.getAssignedTo() != null ? inq.getAssignedTo().getFullName() : "");
                row.createCell(8).setCellValue(inq.getStatus().name());

                // Highlight rows due very soon (within 7 days)
                if (inq.getExpectedCloseDate() != null
                        && !inq.getExpectedCloseDate().isAfter(LocalDate.now().plusDays(7))) {
                    for (int i = 0; i < 9; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(alertStyle);
                } else {
                    for (int i = 0; i < 9; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                }
                row.getCell(5).setCellStyle(currencyStyle);
            }
            for (int i = 0; i < dueHeaders.length; i++) dueSheet.autoSizeColumn(i);

            // ── Sheet 3: All Insurance Records ───────────────────
            XSSFSheet allSheet = wb.createSheet("All Insurance Records");
            Row allTitle = allSheet.createRow(0);
            allTitle.createCell(0).setCellValue("All Insurance Records: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT));
            allTitle.getCell(0).setCellStyle(titleStyle);
            allSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            String[] allHeaders = {"#", "Date", "Customer Name", "Mobile", "Insurance Type",
                                    "Bank/Company", "Premium Amt (₹)", "Tenure", "Status", "Assigned To", "Remarks"};
            Row allHeaderRow = allSheet.createRow(2);
            for (int i = 0; i < allHeaders.length; i++) {
                Cell c = allHeaderRow.createCell(i); c.setCellValue(allHeaders[i]); c.setCellStyle(headerStyle);
            }

            int ar = 3;
            for (Inquiry inq : insuranceInq) {
                Row row = allSheet.createRow(ar++);
                row.createCell(0).setCellValue(ar - 3);
                row.createCell(1).setCellValue(inq.getInquiryDate().format(DATE_FMT));
                row.createCell(2).setCellValue(inq.getCustomer().getCustomerName());
                row.createCell(3).setCellValue(inq.getCustomer().getMobileNumber());
                row.createCell(4).setCellValue(inq.getInquiryType().getTypeName());
                row.createCell(5).setCellValue(inq.getBankName() != null ? inq.getBankName() : "");
                Cell premC = row.createCell(6);
                premC.setCellValue(inq.getLoanAmount() != null ? inq.getLoanAmount().doubleValue() : 0);
                premC.setCellStyle(currencyStyle);
                row.createCell(7).setCellValue(inq.getTenureMonths() != null ? inq.getTenureMonths() + " months" : "");
                row.createCell(8).setCellValue(inq.getStatus().name());
                row.createCell(9).setCellValue(inq.getAssignedTo() != null ? inq.getAssignedTo().getFullName() : "");
                row.createCell(10).setCellValue(inq.getRemarks() != null ? inq.getRemarks() : "");
                for (int i = 0; i < 11; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(6).setCellStyle(currencyStyle);
            }
            for (int i = 0; i < allHeaders.length; i++) allSheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // EXISTING REPORTS (unchanged)
    // ════════════════════════════════════════════════════════════

    public byte[] generateInquiryReportExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Inquiry> inquiries = inquiryRepository.findByDateRange(startDate, endDate);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Inquiry Report");
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Inquiry Report: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT));
            titleCell.setCellStyle(createTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            String[] headers = {"#", "Date", "Customer Name", "Mobile", "Inquiry Type", "Loan Amount", "Bank", "Status", "Assigned To", "Remarks"};
            Row hRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }

            int rowNum = 3;
            for (Inquiry inq : inquiries) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 3);
                row.createCell(1).setCellValue(inq.getInquiryDate().format(DATE_FMT));
                row.createCell(2).setCellValue(inq.getCustomer().getCustomerName());
                row.createCell(3).setCellValue(inq.getCustomer().getMobileNumber());
                row.createCell(4).setCellValue(inq.getInquiryType().getTypeName());
                Cell amtCell = row.createCell(5); amtCell.setCellValue(inq.getLoanAmount() != null ? inq.getLoanAmount().doubleValue() : 0); amtCell.setCellStyle(currencyStyle);
                row.createCell(6).setCellValue(inq.getBankName() != null ? inq.getBankName() : "");
                row.createCell(7).setCellValue(inq.getStatus().name());
                row.createCell(8).setCellValue(inq.getAssignedTo() != null ? inq.getAssignedTo().getFullName() : "");
                row.createCell(9).setCellValue(inq.getRemarks() != null ? inq.getRemarks() : "");
                for (int i = 0; i < 10; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(5).setCellStyle(currencyStyle);
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateExpenseReportExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Expense> expenses = expenseRepository.findByDateRange(startDate, endDate);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Expense Report");
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Expense Report: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT));
            titleRow.getCell(0).setCellStyle(createTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            String[] headers = {"#", "Date", "Category", "Amount (₹)", "Payment Mode", "Vendor", "Bill No."};
            Row hRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }

            int rowNum = 3;
            BigDecimal total = BigDecimal.ZERO;
            for (Expense e : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 3);
                row.createCell(1).setCellValue(e.getExpenseDate().format(DATE_FMT));
                row.createCell(2).setCellValue(e.getCategory().getCategoryName());
                Cell amtCell = row.createCell(3); amtCell.setCellValue(e.getAmount().doubleValue()); amtCell.setCellStyle(currencyStyle);
                row.createCell(4).setCellValue(e.getPaymentMode().name());
                row.createCell(5).setCellValue(e.getVendorName() != null ? e.getVendorName() : "");
                row.createCell(6).setCellValue(e.getBillNumber() != null ? e.getBillNumber() : "");
                total = total.add(e.getAmount());
                for (int i = 0; i < 7; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(3).setCellStyle(currencyStyle);
            }
            Row totalRow = sheet.createRow(rowNum + 1);
            Cell totalLabel = totalRow.createCell(2); totalLabel.setCellValue("TOTAL"); totalLabel.setCellStyle(headerStyle);
            Cell totalAmt = totalRow.createCell(3); totalAmt.setCellValue(total.doubleValue()); totalAmt.setCellStyle(currencyStyle);
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateIncomeReportExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Income> incomes = incomeRepository.findByDateRange(startDate, endDate);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Income Report");
            CellStyle headerStyle   = createHeaderStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle dataStyle     = createDataStyle(wb);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Income Report: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT));
            titleRow.getCell(0).setCellStyle(createTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            String[] headers = {"#", "Date", "Category", "Customer", "Amount (₹)", "Payment Mode", "Ref. No."};
            Row hRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }

            int rowNum = 3;
            BigDecimal total = BigDecimal.ZERO;
            for (Income inc : incomes) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 3);
                row.createCell(1).setCellValue(inc.getIncomeDate().format(DATE_FMT));
                row.createCell(2).setCellValue(inc.getCategory().getCategoryName());
                row.createCell(3).setCellValue(inc.getCustomer() != null ? inc.getCustomer().getCustomerName() : "");
                Cell amtCell = row.createCell(4); amtCell.setCellValue(inc.getAmount().doubleValue()); amtCell.setCellStyle(currencyStyle);
                row.createCell(5).setCellValue(inc.getPaymentMode().name());
                row.createCell(6).setCellValue(inc.getReferenceNumber() != null ? inc.getReferenceNumber() : "");
                total = total.add(inc.getAmount());
                for (int i = 0; i < 7; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
                row.getCell(4).setCellStyle(currencyStyle);
            }
            Row totalRow = sheet.createRow(rowNum + 1);
            Cell totalLabel = totalRow.createCell(3); totalLabel.setCellValue("TOTAL"); totalLabel.setCellStyle(headerStyle);
            Cell totalAmt = totalRow.createCell(4); totalAmt.setCellValue(total.doubleValue()); totalAmt.setCellStyle(currencyStyle);
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateAttendanceReportExcel(Long employeeId, int month, int year) throws IOException {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        List<Attendance> records = attendanceRepository.findByEmployeeAndDateRange(employeeId, start, end);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Attendance");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle   = createDataStyle(wb);

            Row hRow = sheet.createRow(0);
            String[] headers = {"#", "Date", "Employee", "Type", "In Time", "Out Time", "Hours", "Remarks"};
            for (int i = 0; i < headers.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }

            int rowNum = 1;
            for (Attendance a : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(a.getAttendanceDate().format(DATE_FMT));
                row.createCell(2).setCellValue(a.getEmployee().getEmployeeName());
                row.createCell(3).setCellValue(a.getAttendanceType().name());
                row.createCell(4).setCellValue(a.getInTime() != null ? a.getInTime().toString() : "");
                row.createCell(5).setCellValue(a.getOutTime() != null ? a.getOutTime().toString() : "");
                row.createCell(6).setCellValue(a.getWorkingHours() != null ? a.getWorkingHours().toString() : "");
                row.createCell(7).setCellValue(a.getRemarks() != null ? a.getRemarks() : "");
                for (int i = 0; i < 8; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ════════════════════════════════════════════════════════════
    // SHARED SHEET BUILDERS
    // ════════════════════════════════════════════════════════════

    private void buildInquirySheet(XSSFWorkbook wb, List<Inquiry> inquiries, String sheetName,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle currencyStyle, CellStyle titleStyle) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        String[] headers = {"#", "Date", "Customer Name", "Mobile", "Inquiry Type",
                             "Loan Amount (₹)", "Bank", "Status", "Assigned To", "Remarks"};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }
        int r = 1;
        for (Inquiry inq : inquiries) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(r - 1);
            row.createCell(1).setCellValue(inq.getInquiryDate().format(DATE_FMT));
            row.createCell(2).setCellValue(inq.getCustomer().getCustomerName());
            row.createCell(3).setCellValue(inq.getCustomer().getMobileNumber());
            row.createCell(4).setCellValue(inq.getInquiryType().getTypeName());
            Cell amtC = row.createCell(5); amtC.setCellValue(inq.getLoanAmount() != null ? inq.getLoanAmount().doubleValue() : 0); amtC.setCellStyle(currencyStyle);
            row.createCell(6).setCellValue(inq.getBankName() != null ? inq.getBankName() : "");
            row.createCell(7).setCellValue(inq.getStatus().name());
            row.createCell(8).setCellValue(inq.getAssignedTo() != null ? inq.getAssignedTo().getFullName() : "");
            row.createCell(9).setCellValue(inq.getRemarks() != null ? inq.getRemarks() : "");
            for (int i = 0; i < 10; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
            row.getCell(5).setCellStyle(currencyStyle);
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void buildIncomeSheet(XSSFWorkbook wb, List<Income> incomes, String sheetName,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle currencyStyle, CellStyle titleStyle) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        String[] headers = {"#", "Date", "Category", "Customer", "Amount (₹)", "Payment Mode", "Ref. No."};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }
        int r = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (Income inc : incomes) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(r - 1);
            row.createCell(1).setCellValue(inc.getIncomeDate().format(DATE_FMT));
            row.createCell(2).setCellValue(inc.getCategory().getCategoryName());
            row.createCell(3).setCellValue(inc.getCustomer() != null ? inc.getCustomer().getCustomerName() : "");
            Cell amtC = row.createCell(4); amtC.setCellValue(inc.getAmount().doubleValue()); amtC.setCellStyle(currencyStyle);
            row.createCell(5).setCellValue(inc.getPaymentMode().name());
            row.createCell(6).setCellValue(inc.getReferenceNumber() != null ? inc.getReferenceNumber() : "");
            total = total.add(inc.getAmount());
            for (int i = 0; i < 7; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
            row.getCell(4).setCellStyle(currencyStyle);
        }
        Row tRow = sheet.createRow(r + 1);
        Cell tl = tRow.createCell(3); tl.setCellValue("TOTAL"); tl.setCellStyle(headerStyle);
        Cell ta = tRow.createCell(4); ta.setCellValue(total.doubleValue()); ta.setCellStyle(currencyStyle);
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void buildExpenseSheet(XSSFWorkbook wb, List<Expense> expenses, String sheetName,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle currencyStyle, CellStyle titleStyle) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        String[] headers = {"#", "Date", "Category", "Amount (₹)", "Payment Mode", "Vendor", "Bill No."};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }
        int r = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (Expense e : expenses) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(r - 1);
            row.createCell(1).setCellValue(e.getExpenseDate().format(DATE_FMT));
            row.createCell(2).setCellValue(e.getCategory().getCategoryName());
            Cell amtC = row.createCell(3); amtC.setCellValue(e.getAmount().doubleValue()); amtC.setCellStyle(currencyStyle);
            row.createCell(4).setCellValue(e.getPaymentMode().name());
            row.createCell(5).setCellValue(e.getVendorName() != null ? e.getVendorName() : "");
            row.createCell(6).setCellValue(e.getBillNumber() != null ? e.getBillNumber() : "");
            total = total.add(e.getAmount());
            for (int i = 0; i < 7; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
            row.getCell(3).setCellStyle(currencyStyle);
        }
        Row tRow = sheet.createRow(r + 1);
        Cell tl = tRow.createCell(2); tl.setCellValue("TOTAL"); tl.setCellStyle(headerStyle);
        Cell ta = tRow.createCell(3); ta.setCellValue(total.doubleValue()); ta.setCellStyle(currencyStyle);
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void buildFollowupSheet(XSSFWorkbook wb, List<Followup> followups, String sheetName,
            CellStyle headerStyle, CellStyle dataStyle, CellStyle titleStyle) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        String[] headers = {"#", "Follow-up Date", "Customer Name", "Mobile", "Inquiry Type", "Status", "Outcome", "Notes"};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }
        int r = 1;
        for (Followup f : followups) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(r - 1);
            row.createCell(1).setCellValue(f.getFollowupDate().format(DATE_FMT));
            row.createCell(2).setCellValue(f.getCustomer().getCustomerName());
            row.createCell(3).setCellValue(f.getCustomer().getMobileNumber());
            row.createCell(4).setCellValue(f.getInquiry().getInquiryType().getTypeName());
            row.createCell(5).setCellValue(f.getStatus().name());
            row.createCell(6).setCellValue(f.getOutcome() != null ? f.getOutcome().name() : "");
            row.createCell(7).setCellValue(f.getFollowupNotes() != null ? f.getFollowupNotes() : "");
            for (int i = 0; i < 8; i++) if (row.getCell(i) != null) row.getCell(i).setCellStyle(dataStyle);
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    // ════════════════════════════════════════════════════════════
    // HELPER: KPI ROW WRITER
    // ════════════════════════════════════════════════════════════
    private void writeKpiRow(XSSFSheet sheet, int rowNum, String label, String value,
            CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell lc = row.createCell(0); lc.setCellValue(label);    lc.setCellStyle(labelStyle);
        Cell vc = row.createCell(1); vc.setCellValue(value);     vc.setCellStyle(valueStyle);
    }

    // ════════════════════════════════════════════════════════════
    // STYLE HELPERS
    // ════════════════════════════════════════════════════════════

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(XSSFWorkbook wb) {
        CellStyle style = createDataStyle(wb);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createSectionStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createAlertStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}