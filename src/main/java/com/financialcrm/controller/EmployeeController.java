package com.financialcrm.controller;

import com.financialcrm.entity.Employee;
import com.financialcrm.entity.User;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.repository.UserRepository;
import com.financialcrm.service.AttendanceService;
import com.financialcrm.service.EmployeeService;
import com.financialcrm.service.UserService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final SecurityUtils securityUtils;
    private final UserService userService;         // ADD: needed for link-user feature
    private final UserRepository userRepository;   // ADD: needed for link-user feature

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search,
                       Model model) {
        PageRequest pageable = PageRequest.of(page, size);
        var employees = employeeService.search(search, pageable);
        model.addAttribute("employees", employees);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", employees.getTotalPages());
        return "employee/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeService.generateNextEmployeeId());
        employee.setJoiningDate(LocalDate.now());
        model.addAttribute("employee", employee);
        model.addAttribute("genders", Employee.Gender.values());
        return "employee/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@ModelAttribute Employee employee,
                       @RequestParam(value = "photo", required = false) MultipartFile photo,
                       RedirectAttributes redirectAttributes) {
        try {
            User createdBy = securityUtils.getCurrentUserOrThrow();
            employee.setCreatedBy(createdBy);
            employeeService.save(employee, photo);
            redirectAttributes.addFlashAttribute("successMsg",
                "Employee " + employee.getEmployeeName() + " saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error saving employee: " + e.getMessage());
        }
        return "redirect:/employees";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
    	Employee employee = employeeService.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        model.addAttribute("employee", employee);

        // Attendance summary for current month
        LocalDate now = LocalDate.now();
        var attendanceSummary = attendanceService.getMonthlyAttendanceSummary(id, now.getMonthValue(), now.getYear());
        model.addAttribute("attendanceSummary", attendanceSummary);
        model.addAttribute("currentMonth", now.getMonth().name());

        // ADD: pass all users so admin can pick one to link
        model.addAttribute("allUsers", userRepository.findAll());
        return "employee/view";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        model.addAttribute("employee", employee);
        model.addAttribute("genders", Employee.Gender.values());
        return "employee/form";
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @ModelAttribute Employee employee,
                         @RequestParam(value = "photo", required = false) MultipartFile photo,
                         RedirectAttributes redirectAttributes) {
        Employee existing = employeeService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        existing.setEmployeeName(employee.getEmployeeName());
        existing.setFatherName(employee.getFatherName());
        existing.setMotherName(employee.getMotherName());
        existing.setDateOfBirth(employee.getDateOfBirth());
        existing.setGender(employee.getGender());
        existing.setMobileNumber(employee.getMobileNumber());
        existing.setAlternateMobile(employee.getAlternateMobile());
        existing.setEmail(employee.getEmail());
        existing.setAddress(employee.getAddress());
        existing.setCity(employee.getCity());
        existing.setState(employee.getState());
        existing.setPincode(employee.getPincode());
        existing.setDesignation(employee.getDesignation());
        existing.setDepartment(employee.getDepartment());
        existing.setJoiningDate(employee.getJoiningDate());
        existing.setSalary(employee.getSalary());
        existing.setBankAccountNumber(employee.getBankAccountNumber());
        existing.setBankName(employee.getBankName());
        existing.setIfscCode(employee.getIfscCode());
        existing.setPanNumber(employee.getPanNumber());
        existing.setAadharNumber(employee.getAadharNumber());
        existing.setEmergencyContactName(employee.getEmergencyContactName());
        existing.setEmergencyContactMobile(employee.getEmergencyContactMobile());

        try {
            employeeService.save(existing, photo);
            redirectAttributes.addFlashAttribute("successMsg", "Employee updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error updating employee: " + e.getMessage());
        }
        return "redirect:/employees/" + id;
    }

    // ── ADD: Link a User account to this Employee ─────────────────────────────
    @PostMapping("/{id}/link-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String linkUser(@PathVariable Long id,
                           @RequestParam Long userId,
                           RedirectAttributes redirectAttributes) {
        Employee employee = employeeService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        User user = userService.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // If another employee already uses this user, unlink them first
        employeeService.findAllActive().stream()
            .filter(e -> !e.getId().equals(id) && e.getUser() != null && e.getUser().getId().equals(userId))
            .forEach(e -> { e.setUser(null); employeeService.save(e); });

        employee.setUser(user);
        employeeService.save(employee);
        redirectAttributes.addFlashAttribute("successMsg",
            "User '" + user.getFullName() + "' linked to employee '" + employee.getEmployeeName() + "' successfully!");
        return "redirect:/employees/" + id;
    }

    // ── ADD: Unlink User account from this Employee ───────────────────────────
    @PostMapping("/{id}/unlink-user")
    @PreAuthorize("hasRole('ADMIN')")
    public String unlinkUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Employee employee = employeeService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        employee.setUser(null);
        employeeService.save(employee);
        redirectAttributes.addFlashAttribute("successMsg", "User unlinked from employee successfully.");
        return "redirect:/employees/" + id;
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.deactivate(id);
        redirectAttributes.addFlashAttribute("successMsg", "Employee deactivated.");
        return "redirect:/employees";
    }
}