package com.financialcrm.controller;

import com.financialcrm.entity.*;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.repository.AppointmentRepository;
import com.financialcrm.service.*;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final CustomerService customerService;
    private final InquiryService inquiryService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String list(@RequestParam(required = false) String date, Model model) {
        LocalDate viewDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        model.addAttribute("appointments", appointmentRepository.findByDate(viewDate));
        model.addAttribute("viewDate", viewDate);
        model.addAttribute("totalToday", appointmentRepository.countByAppointmentDate(viewDate));
        return "appointment/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long customerId,
                          @RequestParam(required = false) Long inquiryId,
                          Model model) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentDate(LocalDate.now());
        if (customerId != null) {
            customerService.findById(customerId).ifPresent(appointment::setCustomer);
        }
        if (inquiryId != null) {
            inquiryService.findById(inquiryId).ifPresent(appointment::setInquiry);
        }
        populateModel(model);
        model.addAttribute("appointment", appointment);
        return "appointment/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Appointment appointment,
                       @RequestParam Long customerId,
                       @RequestParam(required = false) Long inquiryId,
                       @RequestParam(required = false) Long assignedToId,
                       @RequestParam String appointmentTimeStr,
                       @RequestParam String appointmentTypeStr,
                       RedirectAttributes redirectAttributes) {

        Customer customer = customerService.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        User user = securityUtils.getCurrentUserOrThrow();

        appointment.setCustomer(customer);
        appointment.setCreatedBy(user);
        appointment.setAppointmentTime(LocalTime.parse(appointmentTimeStr));
        appointment.setAppointmentType(Appointment.AppointmentType.valueOf(appointmentTypeStr));
        if (inquiryId != null) inquiryService.findById(inquiryId).ifPresent(appointment::setInquiry);
        if (assignedToId != null) userService.findById(assignedToId).ifPresent(appointment::setAssignedTo);

        appointmentRepository.save(appointment);
        redirectAttributes.addFlashAttribute("successMsg", "Appointment scheduled successfully!");
        return "redirect:/appointments";
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        // Touch lazy associations while the transaction/session is still open
        if (appointment.getCustomer() != null) appointment.getCustomer().getCustomerName();
        if (appointment.getAssignedTo() != null) appointment.getAssignedTo().getFullName();
        if (appointment.getInquiry() != null) appointment.getInquiry().getId();
        populateModel(model);
        model.addAttribute("appointment", appointment);
        return "appointment/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @ModelAttribute Appointment appointment,
                         @RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) Long inquiryId,
                         @RequestParam(required = false) Long assignedToId,
                         @RequestParam(required = false) String appointmentTimeStr,
                         @RequestParam(required = false) String appointmentTypeStr,
                         RedirectAttributes redirectAttributes) {

        Appointment existing = appointmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));

        if (customerId != null) {
            Customer customer = customerService.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
            existing.setCustomer(customer);
        }

        existing.setAppointmentDate(appointment.getAppointmentDate());

        if (appointmentTimeStr != null && !appointmentTimeStr.isBlank()) {
            existing.setAppointmentTime(LocalTime.parse(appointmentTimeStr));
        }
        if (appointmentTypeStr != null && !appointmentTypeStr.isBlank()) {
            existing.setAppointmentType(Appointment.AppointmentType.valueOf(appointmentTypeStr));
        }

        existing.setDurationMinutes(appointment.getDurationMinutes());
        existing.setLocation(appointment.getLocation());
        existing.setDescription(appointment.getDescription());
        existing.setNotes(appointment.getNotes());

        if (inquiryId != null) {
            inquiryService.findById(inquiryId).ifPresent(existing::setInquiry);
        }
        if (assignedToId != null) {
            userService.findById(assignedToId).ifPresent(existing::setAssignedTo);
        } else {
            existing.setAssignedTo(null);
        }

        appointmentRepository.save(existing);
        redirectAttributes.addFlashAttribute("successMsg", "Appointment updated successfully!");
        return "redirect:/appointments";
    }

    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        appointmentRepository.findById(id).ifPresent(a -> {
            a.setStatus(Appointment.Status.valueOf(status));
            appointmentRepository.save(a);
        });
        redirectAttributes.addFlashAttribute("successMsg", "Appointment status updated.");
        return "redirect:/appointments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        appointmentRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Appointment deleted.");
        return "redirect:/appointments";
    }

    private void populateModel(Model model) {
        model.addAttribute("appointmentTypes", Appointment.AppointmentType.values());
        model.addAttribute("statuses", Appointment.Status.values());
        model.addAttribute("allUsers", userService.findAll(PageRequest.of(0, 100)).getContent());
        model.addAttribute("allCustomers", customerService.findAll(PageRequest.of(0, 1000)).getContent());
        model.addAttribute("today", LocalDate.now());
    }
}