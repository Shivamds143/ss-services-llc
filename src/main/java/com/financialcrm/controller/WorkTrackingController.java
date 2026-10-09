package com.financialcrm.controller;

import com.financialcrm.dto.BulkWorkAssignmentFormDTO;
import com.financialcrm.dto.WorkAssignmentFormDTO;
import com.financialcrm.entity.Attendance;
import com.financialcrm.entity.AuditLog;
import com.financialcrm.entity.Employee;
import com.financialcrm.entity.EmployeeDailyWork;
import com.financialcrm.entity.EmployeeDailyWork.WorkPriority;
import com.financialcrm.entity.EmployeeDailyWork.WorkStatus;
import com.financialcrm.entity.EmployeeTarget;
import com.financialcrm.entity.User;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.repository.AuditLogRepository;
import com.financialcrm.repository.EmployeeDailyWorkRepository;
import com.financialcrm.repository.EmployeeTargetRepository;
import com.financialcrm.service.AttendanceService;
import com.financialcrm.service.AuditService;
import com.financialcrm.service.EmployeeService;
import com.financialcrm.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.BindingResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/work-tracking")
@RequiredArgsConstructor
public class WorkTrackingController {

    private static final String WORK_ENTITY_TYPE = "EmployeeDailyWork";

    private final EmployeeDailyWorkRepository dailyWorkRepo;
    private final EmployeeTargetRepository targetRepo;
    private final AuditLogRepository auditLogRepository;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) Long employeeId,
                            @RequestParam(required = false) EmployeeDailyWork.WorkStatus status,
                            @RequestParam(required = false) EmployeeDailyWork.WorkPriority priority,
                            @RequestParam(defaultValue = "latest") String sort,
                            @RequestParam(defaultValue = "false") boolean showDeleted,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model) {
        LocalDate viewDate = date != null ? date : LocalDate.now();
        Employee scopedEmployee = securityUtils.isAdmin() ? null : employeeService.findByUserId(securityUtils.getCurrentUserOrThrow().getId()).orElse(null);
        List<Employee> allEmployees = employeeService.findAllActive();
        List<EmployeeDailyWork> workRecords = dailyWorkRepo.findAllByWorkDate(viewDate, showDeleted);
        Long filterEmployeeId = employeeId;
        if (scopedEmployee != null) {
            allEmployees = allEmployees.stream()
                    .filter(emp -> Objects.equals(emp.getId(), scopedEmployee.getId()))
                    .toList();
            workRecords = workRecords.stream()
                    .filter(work -> Objects.equals(work.getEmployee().getId(), scopedEmployee.getId()))
                    .toList();
            filterEmployeeId = (employeeId != null && !Objects.equals(employeeId, scopedEmployee.getId())) ? null : employeeId;
        }
        Map<Long, EmployeeDailyWork> workRecordMap = workRecords.stream()
                .collect(Collectors.toMap(w -> w.getEmployee().getId(), w -> w));

        final Long effectiveEmployeeId = filterEmployeeId;
        List<Employee> filteredEmployees = allEmployees.stream()
                .filter(emp -> matchesDashboardFilters(emp, workRecordMap.get(emp.getId()), search, effectiveEmployeeId, status, priority, showDeleted))
                .sorted(resolveDashboardSort(workRecordMap, sort))
                .toList();

        Pagination<Employee> pagination = paginate(filteredEmployees, page, size);
        Object[] totals = unwrapTotals(dailyWorkRepo.getTeamTotalsForDate(viewDate));
        long presentCount = attendanceService.countByDateAndType(viewDate, Attendance.AttendanceType.PRESENT);
        Map<String, Object> dashboardSummary = buildDashboardSummary(viewDate, workRecords, dailyWorkRepo.findByDeletedFalseOrderByWorkDateDesc());

        model.addAttribute("viewDate", viewDate);
        model.addAttribute("employees", pagination.items());
        model.addAttribute("allEmployees", allEmployees);
        model.addAttribute("scopedEmployee", scopedEmployee);
        model.addAttribute("workRecordMap", workRecordMap);
        model.addAttribute("teamLeads", totals[0]);
        model.addAttribute("teamCalls", totals[1]);
        model.addAttribute("teamConnected", totals[2]);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("dashboardSummary", dashboardSummary);
        model.addAttribute("statuses", EmployeeDailyWork.WorkStatus.values());
        model.addAttribute("priorities", EmployeeDailyWork.WorkPriority.values());
        model.addAttribute("categories", EmployeeDailyWork.WorkCategory.values());
        model.addAttribute("sort", sort);
        model.addAttribute("search", search);
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("showDeleted", showDeleted);
        model.addAttribute("currentPage", pagination.page());
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", pagination.totalPages());
        model.addAttribute("copyModes", BulkWorkAssignmentFormDTO.CopyMode.values());
        return "work-tracking/dashboard";
    }

    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    public String saveDailyWork(@Valid @ModelAttribute WorkAssignmentFormDTO form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .distinct()
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("errorMsg", errorMessage);
            LocalDate redirectDate = form.getWorkDate() != null ? form.getWorkDate() : LocalDate.now();
            return "redirect:/work-tracking?date=" + redirectDate;
        }
        if (form.getEmployeeId() == null || form.getWorkDate() == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Employee and work date are required.");
            return "redirect:/work-tracking";
        }

        boolean privileged = securityUtils.isAdmin();
        User actingUser = securityUtils.getCurrentUserOrThrow();
        Employee employee = resolveTargetEmployee(form.getEmployeeId(), privileged);

        EmployeeDailyWork work = dailyWorkRepo.findByEmployeeIdAndWorkDate(employee.getId(), form.getWorkDate())
                .orElse(EmployeeDailyWork.builder().employee(employee).workDate(form.getWorkDate()).build());
        boolean isNew = work.getId() == null;
        String oldValue = snapshot(work);

        work.setEmployee(employee);
        work.setWorkDate(form.getWorkDate());

        if (privileged) {
            applyPrivilegedFields(work, form, actingUser);
        } else if (!StringUtils.hasText(work.getWorkTitle())) {
            work.setWorkTitle("My Daily Work");
        }

        applyProgressAndPerformanceFields(work, form);
        normalizeWorkRecord(work, actingUser, privileged);

        EmployeeDailyWork saved = dailyWorkRepo.save(work);
        auditWorkAction(saved, isNew ? "WORK_ASSIGNMENT_CREATED" : "WORK_ASSIGNMENT_UPDATED",
                oldValue, snapshot(saved), request);

        redirectAttributes.addFlashAttribute("successMsg",
                "Work record saved for " + employee.getEmployeeName() + ".");
        if (privileged) {
            if ("employee-report".equalsIgnoreCase(form.getReturnView())) {
                StringBuilder redirect = new StringBuilder("redirect:/work-tracking/employee/")
                        .append(employee.getId());
                if (form.getReturnMonth() != null) {
                    redirect.append("?month=").append(form.getReturnMonth());
                    if (form.getReturnYear() != null) {
                        redirect.append("&year=").append(form.getReturnYear());
                    }
                } else if (form.getReturnYear() != null) {
                    redirect.append("?year=").append(form.getReturnYear());
                }
                if (Boolean.TRUE.equals(form.getReturnIncludeDeleted())) {
                    redirect.append(redirect.indexOf("?") >= 0 ? "&" : "?")
                            .append("includeDeleted=true");
                }
                return redirect.toString();
            }
            return "redirect:/work-tracking?date=" + form.getWorkDate();
        }
        return "redirect:/work-tracking/my-work?date=" + form.getWorkDate();
    }

    @PostMapping("/bulk-assign")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String bulkAssign(@Valid @ModelAttribute BulkWorkAssignmentFormDTO form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .distinct()
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("errorMsg", errorMessage);
            LocalDate redirectDate = form.getStartDate() != null ? form.getStartDate() : LocalDate.now();
            return "redirect:/work-tracking?date=" + redirectDate;
        }
        LocalDate startDate = form.getStartDate() != null ? form.getStartDate() : LocalDate.now();
        LocalDate endDate = resolveBulkEndDate(startDate, form);

        if (endDate.isBefore(startDate)) {
            redirectAttributes.addFlashAttribute("errorMsg", "End date must be after start date.");
            return "redirect:/work-tracking?date=" + startDate;
        }

        List<Employee> targetEmployees = resolveBulkEmployees(form);
        if (targetEmployees.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Select at least one employee for bulk assignment.");
            return "redirect:/work-tracking?date=" + startDate;
        }

        User actingUser = securityUtils.getCurrentUserOrThrow();
        int savedCount = 0;

        for (Employee employee : targetEmployees) {
            for (LocalDate workDate = startDate; !workDate.isAfter(endDate); workDate = workDate.plusDays(1)) {
                EmployeeDailyWork work = dailyWorkRepo.findByEmployeeIdAndWorkDate(employee.getId(), workDate)
                        .orElse(EmployeeDailyWork.builder().employee(employee).workDate(workDate).build());
                boolean isNew = work.getId() == null;
                String oldValue = snapshot(work);

                applyCopyModeIfNeeded(work, employee.getId(), workDate, form.getCopyMode());
                applyBulkAssignmentFields(work, form, actingUser, workDate);
                normalizeWorkRecord(work, actingUser, true);

                EmployeeDailyWork saved = dailyWorkRepo.save(work);
                auditWorkAction(saved, isNew ? "WORK_ASSIGNMENT_CREATED" : "WORK_ASSIGNMENT_BULK_UPDATED",
                        oldValue, snapshot(saved), request);
                savedCount++;
            }
        }

        redirectAttributes.addFlashAttribute("successMsg",
                "Bulk work assignment saved for " + savedCount + " day entries.");
        return "redirect:/work-tracking?date=" + startDate;
    }

    @PostMapping("/copy-previous-day")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String copyPreviousDay(@RequestParam Long employeeId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
                                  RedirectAttributes redirectAttributes,
                                  HttpServletRequest request) {
        return copyRelativeWork(employeeId, workDate, 1, "Previous day work copied successfully.",
                "No previous day work found to copy.", redirectAttributes, request);
    }

    @PostMapping("/duplicate-previous-week")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String duplicatePreviousWeek(@RequestParam Long employeeId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
                                        RedirectAttributes redirectAttributes,
                                        HttpServletRequest request) {
        return copyRelativeWork(employeeId, workDate, 7, "Previous week work duplicated successfully.",
                "No previous week work found to duplicate.", redirectAttributes, request);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String deleteWork(@RequestParam Long workId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate viewDate,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        EmployeeDailyWork work = dailyWorkRepo.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDailyWork", workId));

        String oldValue = snapshot(work);
        User actingUser = securityUtils.getCurrentUserOrThrow();
        work.setDeleted(true);
        work.setDeletedAt(LocalDateTime.now());
        work.setDeletedBy(actingUser);
        dailyWorkRepo.save(work);
        auditWorkAction(work, "WORK_ASSIGNMENT_SOFT_DELETED", oldValue, snapshot(work), request);

        redirectAttributes.addFlashAttribute("successMsg", "Work assignment deleted successfully.");
        return "redirect:/work-tracking?date=" + viewDate;
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String bulkDelete(@RequestParam(name = "workIds", required = false) List<Long> workIds,
                             @RequestParam Long employeeId,
                             @RequestParam int month,
                             @RequestParam int year,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        if (workIds == null || workIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Select work records to delete.");
            return "redirect:/work-tracking/employee/" + employeeId + "?month=" + month + "&year=" + year;
        }

        User actingUser = securityUtils.getCurrentUserOrThrow();
        int deletedCount = 0;
        for (Long workId : workIds) {
            EmployeeDailyWork work = dailyWorkRepo.findById(workId)
                    .orElseThrow(() -> new ResourceNotFoundException("EmployeeDailyWork", workId));
            if (Boolean.TRUE.equals(work.getDeleted())) {
                continue;
            }
            String oldValue = snapshot(work);
            work.setDeleted(true);
            work.setDeletedAt(LocalDateTime.now());
            work.setDeletedBy(actingUser);
            dailyWorkRepo.save(work);
            auditWorkAction(work, "WORK_ASSIGNMENT_BULK_DELETED", oldValue, snapshot(work), request);
            deletedCount++;
        }

        redirectAttributes.addFlashAttribute("successMsg", deletedCount + " work record(s) deleted.");
        return "redirect:/work-tracking/employee/" + employeeId + "?month=" + month + "&year=" + year;
    }

    @PostMapping("/restore")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String restoreWork(@RequestParam Long workId,
                              @RequestParam Long employeeId,
                              @RequestParam int month,
                              @RequestParam int year,
                              RedirectAttributes redirectAttributes,
                              HttpServletRequest request) {
        EmployeeDailyWork work = dailyWorkRepo.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeDailyWork", workId));

        String oldValue = snapshot(work);
        work.setDeleted(false);
        work.setDeletedAt(null);
        work.setDeletedBy(null);
        work.setRestoredAt(LocalDateTime.now());
        dailyWorkRepo.save(work);
        auditWorkAction(work, "WORK_ASSIGNMENT_RESTORED", oldValue, snapshot(work), request);

        redirectAttributes.addFlashAttribute("successMsg", "Deleted work restored successfully.");
        return "redirect:/work-tracking/employee/" + employeeId + "?month=" + month + "&year=" + year + "&includeDeleted=true";
    }

    @GetMapping("/my-work")
    @PreAuthorize("isAuthenticated()")
    public String myWork(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         Model model) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Employee employee = employeeService.findByUserId(currentUser.getId()).orElse(null);
        LocalDate viewDate = date != null ? date : LocalDate.now();

        model.addAttribute("viewDate", viewDate);
        model.addAttribute("statuses", EmployeeDailyWork.WorkStatus.values());
        model.addAttribute("priorities", EmployeeDailyWork.WorkPriority.values());

        if (employee == null) {
            return "work-tracking/my-work";
        }

        LocalDate monthStart = viewDate.withDayOfMonth(1);
        LocalDate monthEnd = viewDate.withDayOfMonth(viewDate.lengthOfMonth());
        List<EmployeeDailyWork> monthLog = dailyWorkRepo.findByEmployeeAndDateRange(employee.getId(), monthStart, monthEnd, false);
        List<EmployeeDailyWork> workWindow = dailyWorkRepo.findByEmployeeAndDateRange(employee.getId(), viewDate.minusDays(30), viewDate.plusDays(15), false);
        EmployeeDailyWork todayWork = dailyWorkRepo.findByEmployeeIdAndWorkDate(employee.getId(), viewDate)
                .orElse(EmployeeDailyWork.builder()
                        .employee(employee)
                        .workDate(viewDate)
                        .dueDate(viewDate)
                        .workStatus(WorkStatus.PENDING)
                        .workPriority(WorkPriority.MEDIUM)
                        .build());

        Object[] monthTotals = unwrapTotals(dailyWorkRepo.getMonthlyTotals(employee.getId(), viewDate.getMonthValue(), viewDate.getYear()));
        EmployeeTarget target = targetRepo.findByEmployeeIdAndTargetMonthAndTargetYear(
                employee.getId(), viewDate.getMonthValue(), viewDate.getYear()).orElse(null);

        List<EmployeeDailyWork> history = workWindow.stream()
                .filter(w -> !w.getWorkDate().isAfter(viewDate))
                .sorted(Comparator.comparing(EmployeeDailyWork::getWorkDate).reversed())
                .limit(12)
                .toList();
        List<EmployeeDailyWork> upcomingWork = workWindow.stream()
                .filter(w -> w.getDueDate() != null && !w.getDueDate().isBefore(viewDate))
                .filter(w -> w.getWorkStatus() != WorkStatus.COMPLETED)
                .sorted(Comparator.comparing(EmployeeDailyWork::getDueDate))
                .limit(6)
                .toList();
        List<EmployeeDailyWork> missedWork = workWindow.stream()
                .filter(EmployeeDailyWork::isOverdue)
                .sorted(Comparator.comparing(EmployeeDailyWork::getDueDate))
                .limit(6)
                .toList();

        Map<String, Object> workSummary = buildEmployeeSummary(monthLog, viewDate);
        List<AuditLog> activityLogs = fetchWorkActivityLogs(monthLog);

        model.addAttribute("employee", employee);
        model.addAttribute("todayWork", todayWork);
        model.addAttribute("workLog", history);
        model.addAttribute("upcomingWork", upcomingWork);
        model.addAttribute("missedWork", missedWork);
        model.addAttribute("monthTotals", monthTotals);
        model.addAttribute("target", target);
        model.addAttribute("workSummary", workSummary);
        model.addAttribute("activityLogs", activityLogs);
        return "work-tracking/my-work";
    }

    @GetMapping("/employee/{empId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String employeeReport(@PathVariable Long empId,
                                 @RequestParam(required = false) Integer month,
                                 @RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filterDate,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) EmployeeDailyWork.WorkStatus status,
                                 @RequestParam(required = false) EmployeeDailyWork.WorkPriority priority,
                                 @RequestParam(defaultValue = "false") boolean includeDeleted,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "8") int size,
                                 Model model) {
        Employee employee = employeeService.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", empId));
        if (!securityUtils.isAdmin()) {
            Employee currentEmployee = employeeService.findByUserId(securityUtils.getCurrentUserOrThrow().getId()).orElse(null);
            if (currentEmployee == null || !Objects.equals(currentEmployee.getId(), employee.getId())) {
                throw new AccessDeniedException("You can view only your own work report.");
            }
        }

        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();
        LocalDate start = LocalDate.of(y, m, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<EmployeeDailyWork> monthLog = dailyWorkRepo.findByEmployeeAndDateRange(empId, start, end, includeDeleted);
        List<EmployeeDailyWork> filteredLog = monthLog.stream()
                .filter(w -> filterDate == null || Objects.equals(w.getWorkDate(), filterDate))
                .filter(w -> status == null || w.getWorkStatus() == status)
                .filter(w -> priority == null || w.getWorkPriority() == priority)
                .filter(w -> matchesWorkSearch(w, search))
                .sorted(Comparator.comparing(EmployeeDailyWork::getWorkDate).reversed()
                        .thenComparing(EmployeeDailyWork::getUpdatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Pagination<EmployeeDailyWork> pagination = paginate(filteredLog, page, size);
        Object[] monthTotals = unwrapTotals(dailyWorkRepo.getMonthlyTotals(empId, m, y));
        EmployeeTarget target = targetRepo.findByEmployeeIdAndTargetMonthAndTargetYear(empId, m, y).orElse(null);
        List<Object[]> attendanceSummary = attendanceService.getMonthlyAttendanceSummary(empId, m, y);
        Map<String, Object> workSummary = buildEmployeeSummary(monthLog.stream()
                .filter(w -> !Boolean.TRUE.equals(w.getDeleted()))
                .toList(), end);
        List<EmployeeDailyWork> visibleMonthLog = monthLog.stream()
                .filter(w -> !Boolean.TRUE.equals(w.getDeleted()))
                .toList();

        model.addAttribute("employee", employee);
        model.addAttribute("workLog", pagination.items());
        model.addAttribute("monthTotals", monthTotals);
        model.addAttribute("target", target);
        model.addAttribute("attendanceSummary", attendanceSummary);
        model.addAttribute("month", m);
        model.addAttribute("year", y);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("filterDate", filterDate);
        model.addAttribute("includeDeleted", includeDeleted);
        model.addAttribute("currentPage", pagination.page());
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", pagination.totalPages());
        model.addAttribute("statuses", EmployeeDailyWork.WorkStatus.values());
        model.addAttribute("priorities", EmployeeDailyWork.WorkPriority.values());
        model.addAttribute("categories", EmployeeDailyWork.WorkCategory.values());
        model.addAttribute("workSummary", workSummary);
        model.addAttribute("timelineRecords", visibleMonthLog.stream()
                .sorted(Comparator.comparing(WorkTrackingController::resolveSortDateTime).reversed())
                .limit(8)
                .toList());
        model.addAttribute("upcomingWork", visibleMonthLog.stream()
                .filter(w -> w.getDueDate() != null && !w.getDueDate().isBefore(LocalDate.now()))
                .filter(w -> w.getWorkStatus() != WorkStatus.COMPLETED)
                .sorted(Comparator.comparing(EmployeeDailyWork::getDueDate))
                .limit(6)
                .toList());
        model.addAttribute("missedWork", visibleMonthLog.stream()
                .filter(EmployeeDailyWork::isOverdue)
                .sorted(Comparator.comparing(EmployeeDailyWork::getDueDate))
                .limit(6)
                .toList());
        model.addAttribute("activityLogs", fetchWorkActivityLogs(monthLog));
        return "work-tracking/employee-report";
    }

    @PostMapping("/target/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveTarget(@RequestParam Long employeeId,
                             @RequestParam int targetMonth,
                             @RequestParam int targetYear,
                             @RequestParam(defaultValue = "0") int leadsTarget,
                             @RequestParam(defaultValue = "0") int callsTarget,
                             @RequestParam(defaultValue = "0") int conversionTarget,
                             @RequestParam(defaultValue = "0") BigDecimal revenueTarget,
                             @RequestParam(required = false) String remarks,
                             RedirectAttributes redirectAttributes) {
        Employee employee = employeeService.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
        User admin = securityUtils.getCurrentUserOrThrow();

        EmployeeTarget target = targetRepo
                .findByEmployeeIdAndTargetMonthAndTargetYear(employeeId, targetMonth, targetYear)
                .orElse(EmployeeTarget.builder().employee(employee)
                        .targetMonth(targetMonth).targetYear(targetYear).build());

        target.setLeadsTarget(leadsTarget);
        target.setCallsTarget(callsTarget);
        target.setConversionTarget(conversionTarget);
        target.setRevenueTarget(revenueTarget);
        target.setRemarks(remarks);
        target.setSetBy(admin);
        targetRepo.save(target);

        redirectAttributes.addFlashAttribute("successMsg", "Target set for " + employee.getEmployeeName());
        return "redirect:/work-tracking/employee/" + employeeId
                + "?month=" + targetMonth + "&year=" + targetYear;
    }

    @PostMapping("/target/update-achieved")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateAchieved(@RequestParam Long targetId,
                                 @RequestParam int leadsAchieved,
                                 @RequestParam int callsAchieved,
                                 @RequestParam int conversionAchieved,
                                 @RequestParam BigDecimal revenueAchieved,
                                 RedirectAttributes redirectAttributes) {
        EmployeeTarget target = targetRepo.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Target", targetId));
        target.setLeadsAchieved(leadsAchieved);
        target.setCallsAchieved(callsAchieved);
        target.setConversionAchieved(conversionAchieved);
        target.setRevenueAchieved(revenueAchieved);
        targetRepo.save(target);
        redirectAttributes.addFlashAttribute("successMsg", "Achieved values updated.");
        return "redirect:/work-tracking/employee/" + target.getEmployee().getId()
                + "?month=" + target.getTargetMonth() + "&year=" + target.getTargetYear();
    }

    private Employee resolveTargetEmployee(Long employeeId, boolean privileged) {
        if (privileged) {
            return employeeService.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
        }

        User currentUser = securityUtils.getCurrentUserOrThrow();
        Employee employee = employeeService.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Your user is not linked to an employee profile."));
        if (!employee.getId().equals(employeeId)) {
            throw new AccessDeniedException("You can update only your own work.");
        }
        return employee;
    }

    private List<Employee> resolveBulkEmployees(BulkWorkAssignmentFormDTO form) {
        if (form.isAssignAllEmployees()) {
            return employeeService.findAllActive();
        }
        if (form.getEmployeeId() == null) {
            return List.of();
        }
        return List.of(employeeService.findById(form.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", form.getEmployeeId())));
    }

    private LocalDate resolveBulkEndDate(LocalDate startDate, BulkWorkAssignmentFormDTO form) {
        if (form.getEndDate() != null) {
            return form.getEndDate();
        }
        if (form.getRangeDays() != null && form.getRangeDays() > 0) {
            return startDate.plusDays(form.getRangeDays() - 1L);
        }
        return startDate;
    }

    private boolean matchesDashboardFilters(Employee employee,
                                            EmployeeDailyWork work,
                                            String search,
                                            Long employeeId,
                                            EmployeeDailyWork.WorkStatus status,
                                            EmployeeDailyWork.WorkPriority priority,
                                            boolean showDeleted) {
        if (employeeId != null && !employee.getId().equals(employeeId)) {
            return false;
        }
        if (!showDeleted && work != null && Boolean.TRUE.equals(work.getDeleted())) {
            return false;
        }
        if (status != null && (work == null || work.getWorkStatus() != status)) {
            return false;
        }
        if (priority != null && (work == null || work.getWorkPriority() != priority)) {
            return false;
        }
        if (!StringUtils.hasText(search)) {
            return true;
        }

        String needle = search.trim().toLowerCase();
        return contains(employee.getEmployeeName(), needle)
                || contains(employee.getEmployeeId(), needle)
                || contains(employee.getDesignation(), needle)
                || contains(work != null ? work.getWorkTitle() : null, needle)
                || contains(work != null ? work.getWorkDescription() : null, needle)
                || contains(work != null ? work.getWorkRemarks() : null, needle)
                || contains(work != null ? work.getEmployeeNotes() : null, needle);
    }

    private Comparator<Employee> resolveDashboardSort(Map<Long, EmployeeDailyWork> workRecordMap, String sort) {
        Comparator<Employee> latestComparator = Comparator
                .comparing((Employee employee) -> resolveSortDateTime(workRecordMap.get(employee.getId())),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Employee::getEmployeeName, String.CASE_INSENSITIVE_ORDER);
        if ("name".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Employee::getEmployeeName, String.CASE_INSENSITIVE_ORDER);
        }
        return latestComparator;
    }

    private void applyPrivilegedFields(EmployeeDailyWork work, WorkAssignmentFormDTO form, User actingUser) {
        if (StringUtils.hasText(form.getWorkTitle())) {
            work.setWorkTitle(form.getWorkTitle().trim());
        }
        if (StringUtils.hasText(form.getWorkDescription())) {
            work.setWorkDescription(form.getWorkDescription().trim());
        }
        if (form.getWorkCategory() != null) {
            work.setWorkCategory(form.getWorkCategory());
        }
        if (form.getWorkPriority() != null) {
            work.setWorkPriority(form.getWorkPriority());
        }
        if (form.getWorkStatus() != null) {
            work.setWorkStatus(form.getWorkStatus());
        }
        if (form.getDueDate() != null) {
            work.setDueDate(form.getDueDate());
        }
        if (form.getExpectedCallsTarget() != null) {
            work.setExpectedCallsTarget(nonNegative(form.getExpectedCallsTarget()));
        }
        if (form.getExpectedLeadsTarget() != null) {
            work.setExpectedLeadsTarget(nonNegative(form.getExpectedLeadsTarget()));
        }
        if (form.getAdminNotes() != null) {
            work.setAdminNotes(trimToNull(form.getAdminNotes()));
        }
        work.setAssignedBy(actingUser);
        if (work.getAssignedDateTime() == null) {
            work.setAssignedDateTime(LocalDateTime.now());
        }
    }

    private void applyBulkAssignmentFields(EmployeeDailyWork work,
                                           BulkWorkAssignmentFormDTO form,
                                           User actingUser,
                                           LocalDate workDate) {
        work.setWorkDate(workDate);
        if (StringUtils.hasText(form.getWorkTitle())) {
            work.setWorkTitle(form.getWorkTitle().trim());
        }
        if (StringUtils.hasText(form.getWorkDescription())) {
            work.setWorkDescription(form.getWorkDescription().trim());
        }
        if (form.getWorkCategory() != null) {
            work.setWorkCategory(form.getWorkCategory());
        }
        if (form.getWorkPriority() != null) {
            work.setWorkPriority(form.getWorkPriority());
        }
        if (form.getWorkStatus() != null) {
            work.setWorkStatus(form.getWorkStatus());
        }
        if (form.getExpectedCallsTarget() != null) {
            work.setExpectedCallsTarget(nonNegative(form.getExpectedCallsTarget()));
        }
        if (form.getExpectedLeadsTarget() != null) {
            work.setExpectedLeadsTarget(nonNegative(form.getExpectedLeadsTarget()));
        }
        if (form.getWorkRemarks() != null) {
            work.setWorkRemarks(trimToNull(form.getWorkRemarks()));
        }
        if (form.getWorkProgress() != null) {
            work.setWorkProgress(trimToNull(form.getWorkProgress()));
        }
        if (form.getCompletionPercentage() != null) {
            work.setCompletionPercentage(clampPercentage(form.getCompletionPercentage()));
        }
        if (form.getAdminNotes() != null) {
            work.setAdminNotes(trimToNull(form.getAdminNotes()));
        }
        if (!StringUtils.hasText(work.getWorkTitle())) {
            work.setWorkTitle("Assigned Work - " + workDate);
        }
        if (work.getDueDate() == null) {
            work.setDueDate(workDate);
        }
        work.setAssignedBy(actingUser);
        work.setAssignedDateTime(LocalDateTime.now());
        work.setDeleted(false);
        work.setDeletedAt(null);
        work.setDeletedBy(null);
    }

    private void applyProgressAndPerformanceFields(EmployeeDailyWork work, WorkAssignmentFormDTO form) {
        work.setDailyLeads(nonNegative(form.getDailyLeads()));
        work.setLeadsNotes(trimToNull(form.getLeadsNotes()));
        work.setTotalCalls(nonNegative(form.getTotalCalls()));
        work.setConnectedCalls(nonNegative(form.getConnectedCalls()));
        work.setNotAnswered(nonNegative(form.getNotAnswered()));
        work.setBusyCalls(nonNegative(form.getBusyCalls()));
        work.setCallNotes(trimToNull(form.getCallNotes()));
        work.setWorkSummary(trimToNull(form.getWorkSummary()));
        work.setWorkRemarks(trimToNull(form.getWorkRemarks()));
        work.setWorkProgress(trimToNull(form.getWorkProgress()));
        work.setEmployeeNotes(trimToNull(form.getEmployeeNotes()));
        if (form.getWorkStatus() != null) {
            work.setWorkStatus(form.getWorkStatus());
        }
        if (form.getCompletionPercentage() != null) {
            work.setCompletionPercentage(clampPercentage(form.getCompletionPercentage()));
        }
    }

    private void normalizeWorkRecord(EmployeeDailyWork work, User actingUser, boolean privileged) {
        if (!StringUtils.hasText(work.getWorkTitle())) {
            work.setWorkTitle("Assigned Work");
        }
        if (work.getWorkCategory() == null) {
            work.setWorkCategory(EmployeeDailyWork.WorkCategory.OTHER);
        }
        if (work.getWorkPriority() == null) {
            work.setWorkPriority(WorkPriority.MEDIUM);
        }
        if (work.getWorkStatus() == null) {
            work.setWorkStatus(WorkStatus.PENDING);
        }
        if (work.getDueDate() == null) {
            work.setDueDate(work.getWorkDate());
        }
        if (work.getAssignedDateTime() == null) {
            work.setAssignedDateTime(LocalDateTime.now());
        }
        if (work.getAssignedBy() == null && privileged) {
            work.setAssignedBy(actingUser);
        }
        if (work.getDeleted() == null) {
            work.setDeleted(false);
        }
        if (work.getExpectedCallsTarget() == null) {
            work.setExpectedCallsTarget(0);
        }
        if (work.getExpectedLeadsTarget() == null) {
            work.setExpectedLeadsTarget(0);
        }
        if (work.getDailyLeads() == null) {
            work.setDailyLeads(0);
        }
        if (work.getTotalCalls() == null) {
            work.setTotalCalls(0);
        }
        if (work.getConnectedCalls() == null) {
            work.setConnectedCalls(0);
        }
        if (work.getNotAnswered() == null) {
            work.setNotAnswered(0);
        }
        if (work.getBusyCalls() == null) {
            work.setBusyCalls(0);
        }
        if (work.getCompletionPercentage() == null) {
            work.setCompletionPercentage(0);
        }

        if (work.getWorkStatus() == WorkStatus.COMPLETED) {
            work.setCompletionPercentage(100);
            if (work.getCompletedDateTime() == null) {
                work.setCompletedDateTime(LocalDateTime.now());
            }
            if (!StringUtils.hasText(work.getWorkProgress())) {
                work.setWorkProgress("Completed");
            }
        } else {
            work.setCompletedDateTime(null);
            if (work.getWorkStatus() == WorkStatus.PENDING && !StringUtils.hasText(work.getWorkProgress())) {
                work.setWorkProgress("Pending review");
            }
        }
    }

    private void applyCopyModeIfNeeded(EmployeeDailyWork target,
                                       Long employeeId,
                                       LocalDate workDate,
                                       BulkWorkAssignmentFormDTO.CopyMode copyMode) {
        if (copyMode == null || copyMode == BulkWorkAssignmentFormDTO.CopyMode.NONE) {
            return;
        }
        long days = copyMode == BulkWorkAssignmentFormDTO.CopyMode.PREVIOUS_WEEK ? 7 : 1;
        dailyWorkRepo.findByEmployeeIdAndWorkDate(employeeId, workDate.minusDays(days))
                .ifPresent(source -> copyAssignmentTemplate(target, source));
    }

    private void copyAssignmentTemplate(EmployeeDailyWork target, EmployeeDailyWork source) {
        target.setWorkTitle(source.getWorkTitle());
        target.setWorkDescription(source.getWorkDescription());
        target.setWorkCategory(source.getWorkCategory());
        target.setWorkPriority(source.getWorkPriority());
        target.setWorkStatus(source.getWorkStatus());
        target.setWorkRemarks(source.getWorkRemarks());
        target.setExpectedCallsTarget(source.getExpectedCallsTarget());
        target.setExpectedLeadsTarget(source.getExpectedLeadsTarget());
        target.setWorkProgress(source.getWorkProgress());
        target.setCompletionPercentage(source.getCompletionPercentage());
        target.setAdminNotes(source.getAdminNotes());
        target.setEmployeeNotes(source.getEmployeeNotes());
        target.setCallNotes(source.getCallNotes());
        target.setWorkSummary(source.getWorkSummary());
        target.setLeadsNotes(source.getLeadsNotes());
        target.setDailyLeads(source.getDailyLeads());
        target.setTotalCalls(source.getTotalCalls());
        target.setConnectedCalls(source.getConnectedCalls());
        target.setNotAnswered(source.getNotAnswered());
        target.setBusyCalls(source.getBusyCalls());
        target.setDueDate(target.getWorkDate());
    }

    private String copyRelativeWork(Long employeeId,
                                    LocalDate workDate,
                                    long daysBack,
                                    String successMessage,
                                    String emptyMessage,
                                    RedirectAttributes redirectAttributes,
                                    HttpServletRequest request) {
        Employee employee = employeeService.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
        EmployeeDailyWork source = dailyWorkRepo.findVisibleByEmployeeIdAndWorkDate(employeeId, workDate.minusDays(daysBack))
                .orElse(null);

        if (source == null) {
            redirectAttributes.addFlashAttribute("errorMsg", emptyMessage);
            return "redirect:/work-tracking?date=" + workDate;
        }

        EmployeeDailyWork target = dailyWorkRepo.findByEmployeeIdAndWorkDate(employeeId, workDate)
                .orElse(EmployeeDailyWork.builder().employee(employee).workDate(workDate).build());
        String oldValue = snapshot(target);
        boolean isNew = target.getId() == null;

        copyAssignmentTemplate(target, source);
        target.setEmployee(employee);
        target.setWorkDate(workDate);
        target.setAssignedBy(securityUtils.getCurrentUserOrThrow());
        target.setAssignedDateTime(LocalDateTime.now());
        target.setDeleted(false);
        target.setDeletedAt(null);
        target.setDeletedBy(null);
        normalizeWorkRecord(target, securityUtils.getCurrentUserOrThrow(), true);

        EmployeeDailyWork saved = dailyWorkRepo.save(target);
        auditWorkAction(saved, isNew ? "WORK_ASSIGNMENT_CREATED" : "WORK_ASSIGNMENT_COPIED",
                oldValue, snapshot(saved), request);

        redirectAttributes.addFlashAttribute("successMsg", successMessage);
        return "redirect:/work-tracking?date=" + workDate;
    }

    private Map<String, Object> buildDashboardSummary(LocalDate viewDate,
                                                      List<EmployeeDailyWork> todayRecords,
                                                      List<EmployeeDailyWork> allVisibleRecords) {
        Map<String, Object> summary = new LinkedHashMap<>();
        long pending = todayRecords.stream().filter(w -> w.getWorkStatus() == WorkStatus.PENDING).count();
        long inProgress = todayRecords.stream().filter(w -> w.getWorkStatus() == WorkStatus.IN_PROGRESS).count();
        long completed = todayRecords.stream().filter(w -> w.getWorkStatus() == WorkStatus.COMPLETED).count();
        long overdue = todayRecords.stream().filter(EmployeeDailyWork::isOverdue).count();
        long upcoming = allVisibleRecords.stream()
                .filter(w -> w.getDueDate() != null)
                .filter(w -> w.getDueDate().isAfter(viewDate))
                .filter(w -> w.getWorkStatus() != WorkStatus.COMPLETED)
                .count();
        long missed = allVisibleRecords.stream().filter(EmployeeDailyWork::isOverdue).count();
        int averageCompletion = todayRecords.isEmpty() ? 0
                : (int) Math.round(todayRecords.stream()
                .mapToInt(EmployeeDailyWork::getSafeCompletionPercentage)
                .average().orElse(0));

        summary.put("pending", pending);
        summary.put("inProgress", inProgress);
        summary.put("completed", completed);
        summary.put("overdue", overdue);
        summary.put("todayAssigned", todayRecords.size());
        summary.put("upcoming", upcoming);
        summary.put("missed", missed);
        summary.put("averageCompletion", averageCompletion);
        return summary;
    }

    private Map<String, Object> buildEmployeeSummary(List<EmployeeDailyWork> records, LocalDate referenceDate) {
        Map<String, Object> summary = new LinkedHashMap<>();
        long pending = records.stream().filter(w -> w.getWorkStatus() == WorkStatus.PENDING).count();
        long inProgress = records.stream().filter(w -> w.getWorkStatus() == WorkStatus.IN_PROGRESS).count();
        long completed = records.stream().filter(w -> w.getWorkStatus() == WorkStatus.COMPLETED).count();
        long overdue = records.stream().filter(EmployeeDailyWork::isOverdue).count();
        long upcoming = records.stream()
                .filter(w -> w.getDueDate() != null && !w.getDueDate().isBefore(referenceDate))
                .filter(w -> w.getWorkStatus() != WorkStatus.COMPLETED)
                .count();
        int averageCompletion = records.isEmpty() ? 0
                : (int) Math.round(records.stream().mapToInt(EmployeeDailyWork::getSafeCompletionPercentage).average().orElse(0));
        int productivityScore = records.isEmpty() ? 0
                : (int) Math.round(records.stream().mapToInt(this::calculateProductivityScore).average().orElse(0));
        String performanceIndicator = resolvePerformanceIndicator(productivityScore, overdue, completed, records.size());

        summary.put("pending", pending);
        summary.put("inProgress", inProgress);
        summary.put("completed", completed);
        summary.put("overdue", overdue);
        summary.put("upcoming", upcoming);
        summary.put("averageCompletion", averageCompletion);
        summary.put("productivityScore", productivityScore);
        summary.put("performanceIndicator", performanceIndicator);
        summary.put("todayAssigned", records.stream().filter(w -> Objects.equals(w.getWorkDate(), LocalDate.now())).count());
        return summary;
    }

    private int calculateProductivityScore(EmployeeDailyWork work) {
        int completion = work.getSafeCompletionPercentage();
        int callsScore = percentageAgainstTarget(work.getTotalCalls(), work.getExpectedCallsTarget());
        int leadsScore = percentageAgainstTarget(work.getDailyLeads(), work.getExpectedLeadsTarget());
        return (int) Math.round((completion + callsScore + leadsScore) / 3.0);
    }

    private int percentageAgainstTarget(Integer actual, Integer target) {
        int safeActual = actual == null ? 0 : actual;
        int safeTarget = target == null ? 0 : target;
        if (safeTarget <= 0) {
            return safeActual > 0 ? 100 : 0;
        }
        return Math.min(100, (int) Math.round((safeActual * 100.0) / safeTarget));
    }

    private String resolvePerformanceIndicator(int productivityScore, long overdue, long completed, int totalRecords) {
        int completionRate = totalRecords == 0 ? 0 : (int) Math.round((completed * 100.0) / totalRecords);
        if (productivityScore >= 85 && overdue == 0 && completionRate >= 80) {
            return "Excellent";
        }
        if (productivityScore >= 70 && completionRate >= 65) {
            return "Good";
        }
        if (productivityScore >= 50) {
            return "Average";
        }
        return "Needs Attention";
    }

    private List<AuditLog> fetchWorkActivityLogs(List<EmployeeDailyWork> workRecords) {
        List<Long> workIds = workRecords.stream()
                .map(EmployeeDailyWork::getId)
                .filter(Objects::nonNull)
                .toList();
        if (workIds.isEmpty()) {
            return new ArrayList<>();
        }
        return auditLogRepository.findTop20ByEntityTypeAndEntityIdInOrderByCreatedAtDesc(WORK_ENTITY_TYPE, workIds);
    }

    private void auditWorkAction(EmployeeDailyWork work,
                                 String action,
                                 String oldValue,
                                 String newValue,
                                 HttpServletRequest request) {
        auditService.log(securityUtils.getCurrentUsername(),
                action,
                WORK_ENTITY_TYPE,
                work.getId(),
                oldValue,
                newValue,
                SecurityUtils.getClientIp(request),
                AuditLog.Status.SUCCESS);
    }

    private String snapshot(EmployeeDailyWork work) {
        if (work == null) {
            return "";
        }
        return "date=" + work.getWorkDate()
                + ", title=" + safe(work.getWorkTitle())
                + ", status=" + String.valueOf(work.getWorkStatus())
                + ", priority=" + String.valueOf(work.getWorkPriority())
                + ", dueDate=" + String.valueOf(work.getDueDate())
                + ", completion=" + work.getSafeCompletionPercentage()
                + ", leads=" + nonNegative(work.getDailyLeads())
                + ", calls=" + nonNegative(work.getTotalCalls())
                + ", deleted=" + Boolean.TRUE.equals(work.getDeleted());
    }

    private boolean matchesWorkSearch(EmployeeDailyWork work, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String needle = search.trim().toLowerCase();
        return contains(work.getWorkTitle(), needle)
                || contains(work.getWorkDescription(), needle)
                || contains(work.getWorkCategory() != null ? work.getWorkCategory().name().replace('_', ' ') : null, needle)
                || contains(work.getWorkStatus() != null ? work.getWorkStatus().name().replace('_', ' ') : null, needle)
                || contains(work.getWorkPriority() != null ? work.getWorkPriority().name().replace('_', ' ') : null, needle)
                || contains(work.getWorkRemarks(), needle)
                || contains(work.getWorkProgress(), needle)
                || contains(work.getAdminNotes(), needle)
                || contains(work.getEmployeeNotes(), needle)
                || contains(work.getWorkSummary(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private static LocalDateTime resolveSortDateTime(EmployeeDailyWork work) {
        if (work == null) {
            return null;
        }
        if (work.getUpdatedAt() != null) {
            return work.getUpdatedAt();
        }
        if (work.getAssignedDateTime() != null) {
            return work.getAssignedDateTime();
        }
        return work.getCreatedAt();
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private int clampPercentage(Integer value) {
        return value == null ? 0 : Math.max(0, Math.min(100, value));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
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

    private <T> Pagination<T> paginate(List<T> items, int page, int size) {
        int safeSize = Math.max(1, size);
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) safeSize));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(safePage * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());
        return new Pagination<>(items.subList(fromIndex, toIndex), safePage, totalPages);
    }

    private record Pagination<T>(List<T> items, int page, int totalPages) {
    }
}
