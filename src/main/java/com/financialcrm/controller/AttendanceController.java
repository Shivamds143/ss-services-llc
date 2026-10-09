package com.financialcrm.controller;

import com.financialcrm.entity.Attendance;
import com.financialcrm.entity.Employee;
import com.financialcrm.entity.User;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.service.AttendanceService;
import com.financialcrm.service.EmployeeService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String dailyView(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        LocalDate viewDate = (date != null) ? date : LocalDate.now();
        List<Attendance> records   = attendanceService.findByDate(viewDate);
        List<Employee>   allEmployees = employeeService.findAllActive();

        // Build employeeId -> Attendance map so Thymeleaf can do simple lookup
        Map<Long, Attendance> attendanceMap = new HashMap<>();
        for (Attendance a : records) {
            attendanceMap.put(a.getEmployee().getId(), a);
        }

        model.addAttribute("records",         records);
        model.addAttribute("attendanceMap",   attendanceMap);
        model.addAttribute("allEmployees",    allEmployees);
        model.addAttribute("viewDate",        viewDate);
        model.addAttribute("attendanceTypes", Attendance.AttendanceType.values());
        model.addAttribute("presentCount",
            attendanceService.countByDateAndType(viewDate, Attendance.AttendanceType.PRESENT));
        model.addAttribute("absentCount",
            attendanceService.countByDateAndType(viewDate, Attendance.AttendanceType.ABSENT));
        model.addAttribute("halfDayCount",
            attendanceService.countByDateAndType(viewDate, Attendance.AttendanceType.HALF_DAY));
        model.addAttribute("leaveCount",
            attendanceService.countByDateAndType(viewDate, Attendance.AttendanceType.LEAVE));
        model.addAttribute("holidayCount",
            attendanceService.countByDateAndType(viewDate, Attendance.AttendanceType.HOLIDAY));
        model.addAttribute("weekOffCount",
            attendanceService.countByDateAndType(viewDate, Attendance.AttendanceType.WEEK_OFF));

        return "attendance/daily";
    }

    @PostMapping("/mark")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String markAttendance(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
            @RequestParam String attendanceType,
            @RequestParam(required = false) String inTime,
            @RequestParam(required = false) String outTime,
            @RequestParam(required = false) String remarks,
            RedirectAttributes redirectAttributes) {

        Employee employee = employeeService.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
        User user = securityUtils.getCurrentUserOrThrow();

        attendanceService.findByEmployeeAndDate(employeeId, attendanceDate).ifPresentOrElse(
            existing -> {
                existing.setAttendanceType(Attendance.AttendanceType.valueOf(attendanceType));
                existing.setInTime(inTime  != null && !inTime.isEmpty()  ? LocalTime.parse(inTime)  : null);
                existing.setOutTime(outTime != null && !outTime.isEmpty() ? LocalTime.parse(outTime) : null);
                existing.setRemarks(remarks);
                attendanceService.save(existing);
            },
            () -> {
                Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .attendanceDate(attendanceDate)
                    .attendanceType(Attendance.AttendanceType.valueOf(attendanceType))
                    .inTime(inTime  != null && !inTime.isEmpty()  ? LocalTime.parse(inTime)  : null)
                    .outTime(outTime != null && !outTime.isEmpty() ? LocalTime.parse(outTime) : null)
                    .remarks(remarks)
                    .createdBy(user)
                    .build();
                attendanceService.save(attendance);
            }
        );

        redirectAttributes.addFlashAttribute("successMsg", "Attendance marked successfully.");
        return "redirect:/attendance?date=" + attendanceDate;
    }

    @PostMapping("/bulk-mark")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String bulkMark(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "PRESENT") String defaultType,
            RedirectAttributes redirectAttributes) {

        List<Employee> employees = employeeService.findAllActive();
        User user = securityUtils.getCurrentUserOrThrow();
        for (Employee emp : employees) {
            if (attendanceService.findByEmployeeAndDate(emp.getId(), date).isEmpty()) {
                attendanceService.save(Attendance.builder()
                    .employee(emp)
                    .attendanceDate(date)
                    .attendanceType(Attendance.AttendanceType.valueOf(defaultType))
                    .createdBy(user)
                    .build());
            }
        }
        redirectAttributes.addFlashAttribute("successMsg", "Bulk attendance marked as " + defaultType);
        return "redirect:/attendance?date=" + date;
    }

    @GetMapping("/employee/{empId}")
    public String employeeReport(
            @PathVariable Long empId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        Employee employee = employeeService.findById(empId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", empId));

        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year  != null) ? year  : now.getYear();

        LocalDate start = LocalDate.of(y, m, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());

        model.addAttribute("employee", employee);
        model.addAttribute("records",  attendanceService.findByEmployeeAndDateRange(empId, start, end));
        model.addAttribute("summary",  attendanceService.getMonthlyAttendanceSummary(empId, m, y));
        model.addAttribute("month",    m);
        model.addAttribute("year",     y);
        return "attendance/employee-report";
    }
}