package com.financialcrm.controller;

import com.financialcrm.entity.*;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.repository.InquiryTypeRepository;
import com.financialcrm.service.*;
import com.financialcrm.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final CustomerService customerService;
    private final InquiryTypeRepository inquiryTypeRepository;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String status,
                       // NEW: today filter toggle — ?todayOnly=true shows only today's inquiries
                       @RequestParam(required = false, defaultValue = "false") boolean todayOnly,
                       Model model) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Inquiry.Status statusEnum = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                statusEnum = Inquiry.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<Inquiry> inquiries;
        if (todayOnly) {
            inquiries = inquiryService.findTodayInquiries(search, statusEnum, pageable);
        } else {
            inquiries = inquiryService.searchByStatus(search, statusEnum, pageable);
        }

        // Badge count for "Today" button
        long todayCount = inquiryService.countTodayInquiries();

        model.addAttribute("inquiries",      inquiries);
        model.addAttribute("search",         search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses",       Inquiry.Status.values());
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     inquiries.getTotalPages());
        model.addAttribute("totalElements",  inquiries.getTotalElements());
        model.addAttribute("todayOnly",      todayOnly);
        model.addAttribute("todayCount",     todayCount);
        return "inquiry/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long customerId, Model model) {
        Inquiry inquiry = new Inquiry();
        if (customerId != null) {
            customerService.findById(customerId).ifPresent(inquiry::setCustomer);
        }
        populateFormModel(model);
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("preselectedCustomerId", customerId);
        return "inquiry/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Inquiry inquiry,
                        BindingResult result,
                        @RequestParam(required = false) Long customerId,
                        @RequestParam(required = false) Long inquiryTypeId,
                        @RequestParam(required = false) Long assignedToId,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        if (customerId == null) result.rejectValue("customer", "required", "Please select a customer");
        if (inquiryTypeId == null) result.rejectValue("inquiryType", "required", "Please select an inquiry type");
        if (inquiry.getInquiryDate() == null) result.rejectValue("inquiryDate", "required", "Inquiry date is required");

        if (result.hasErrors()) {
            if (customerId != null) customerService.findById(customerId).ifPresent(inquiry::setCustomer);
            if (inquiryTypeId != null) inquiryTypeRepository.findById(inquiryTypeId).ifPresent(inquiry::setInquiryType);
            if (assignedToId != null) userService.findById(assignedToId).ifPresent(inquiry::setAssignedTo);
            populateFormModel(model);
            return "inquiry/form";
        }

        customerService.findById(customerId).ifPresent(inquiry::setCustomer);
        inquiryTypeRepository.findById(inquiryTypeId).ifPresent(inquiry::setInquiryType);
        if (assignedToId != null) userService.findById(assignedToId).ifPresent(inquiry::setAssignedTo);

        inquiryService.save(inquiry);
        redirectAttributes.addFlashAttribute("successMsg", "Inquiry created successfully!");
        return "redirect:/inquiries";
    }
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Inquiry inquiry = inquiryService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inquiry", id));
        model.addAttribute("inquiry", inquiry);
        return "inquiry/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Inquiry inquiry = inquiryService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inquiry", id));
        populateFormModel(model);
        model.addAttribute("inquiry", inquiry);
        return "inquiry/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @ModelAttribute Inquiry inquiry,
                         BindingResult result,
                         @RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) Long inquiryTypeId,
                         @RequestParam(required = false) Long assignedToId,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (customerId == null) result.rejectValue("customer", "required", "Please select a customer");
        if (inquiryTypeId == null) result.rejectValue("inquiryType", "required", "Please select an inquiry type");
        if (inquiry.getInquiryDate() == null) result.rejectValue("inquiryDate", "required", "Inquiry date is required");

        if (result.hasErrors()) {
            if (customerId != null) customerService.findById(customerId).ifPresent(inquiry::setCustomer);
            if (inquiryTypeId != null) inquiryTypeRepository.findById(inquiryTypeId).ifPresent(inquiry::setInquiryType);
            if (assignedToId != null) userService.findById(assignedToId).ifPresent(inquiry::setAssignedTo);
            inquiry.setId(id);
            populateFormModel(model);
            return "inquiry/form";
        }

        Inquiry existing = inquiryService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inquiry", id));

        // Core CRM fields
        customerService.findById(customerId).ifPresent(existing::setCustomer);
        inquiryTypeRepository.findById(inquiryTypeId).ifPresent(existing::setInquiryType);
        existing.setInquiryDate(inquiry.getInquiryDate());
        existing.setLoanAmount(inquiry.getLoanAmount());
        existing.setStatus(inquiry.getStatus());
        existing.setPriority(inquiry.getPriority());
        existing.setBankName(inquiry.getBankName());
        existing.setInterestRate(inquiry.getInterestRate());
        existing.setTenureMonths(inquiry.getTenureMonths());
        existing.setEmiAmount(inquiry.getEmiAmount());
        existing.setRemarks(inquiry.getRemarks());
        existing.setExpectedCloseDate(inquiry.getExpectedCloseDate());
        existing.setRejectionReason(inquiry.getRejectionReason());
        if (assignedToId != null) {
            userService.findById(assignedToId).ifPresent(existing::setAssignedTo);
        } else {
            existing.setAssignedTo(null);
        }
        if (inquiry.getStatus() == Inquiry.Status.APPROVED || inquiry.getStatus() == Inquiry.Status.REJECTED) {
            existing.setActualCloseDate(LocalDate.now());
        }

        // LIC HFL applicant personal
        existing.setApplicantPan(inquiry.getApplicantPan());
        existing.setApplicantNameAsPan(inquiry.getApplicantNameAsPan());
        existing.setApplicantSurname(inquiry.getApplicantSurname());
        existing.setApplicantFirstName(inquiry.getApplicantFirstName());
        existing.setApplicantMiddleName(inquiry.getApplicantMiddleName());
        existing.setApplicantFatherHusbandName(inquiry.getApplicantFatherHusbandName());
        existing.setApplicantDob(inquiry.getApplicantDob());
        existing.setApplicantGender(inquiry.getApplicantGender());
        existing.setApplicantMaritalStatus(inquiry.getApplicantMaritalStatus());
        existing.setApplicantResidencyStatus(inquiry.getApplicantResidencyStatus());
        existing.setApplicantCategory(inquiry.getApplicantCategory());
        existing.setApplicantEducation(inquiry.getApplicantEducation());
        existing.setApplicantInstitute(inquiry.getApplicantInstitute());
        existing.setApplicantDependentsChildren(inquiry.getApplicantDependentsChildren());
        existing.setApplicantDependentsAdults(inquiry.getApplicantDependentsAdults());

        // LIC HFL applicant address
        existing.setApplicantFlatNo(inquiry.getApplicantFlatNo());
        existing.setApplicantBuilding(inquiry.getApplicantBuilding());
        existing.setApplicantRoad(inquiry.getApplicantRoad());
        existing.setApplicantLandmark(inquiry.getApplicantLandmark());
        existing.setApplicantArea(inquiry.getApplicantArea());
        existing.setApplicantCity(inquiry.getApplicantCity());
        existing.setApplicantState(inquiry.getApplicantState());
        existing.setApplicantPin(inquiry.getApplicantPin());
        existing.setApplicantPhone(inquiry.getApplicantPhone());
        existing.setApplicantYearsAtResidence(inquiry.getApplicantYearsAtResidence());
        existing.setApplicantResidenceStatus(inquiry.getApplicantResidenceStatus());

        // LIC HFL applicant income
        existing.setApplicantOccupation(inquiry.getApplicantOccupation());
        existing.setApplicantEmployerName(inquiry.getApplicantEmployerName());
        existing.setApplicantDesignation(inquiry.getApplicantDesignation());
        existing.setApplicantEmployeeNo(inquiry.getApplicantEmployeeNo());
        existing.setApplicantDepartment(inquiry.getApplicantDepartment());
        existing.setApplicantDateOfJoining(inquiry.getApplicantDateOfJoining());
        existing.setApplicantTotalExperience(inquiry.getApplicantTotalExperience());
        existing.setApplicantRetirementAge(inquiry.getApplicantRetirementAge());
        existing.setApplicantGrossAnnualIncome(inquiry.getApplicantGrossAnnualIncome());
        existing.setApplicantNetAnnualIncome(inquiry.getApplicantNetAnnualIncome());
        existing.setApplicantNatureOfBusiness(inquiry.getApplicantNatureOfBusiness());
        existing.setApplicantYearsInBusiness(inquiry.getApplicantYearsInBusiness());

        // LIC HFL co-applicant
        existing.setCoapplicantPan(inquiry.getCoapplicantPan());
        existing.setCoapplicantNameAsPan(inquiry.getCoapplicantNameAsPan());
        existing.setCoapplicantSurname(inquiry.getCoapplicantSurname());
        existing.setCoapplicantFirstName(inquiry.getCoapplicantFirstName());
        existing.setCoapplicantMiddleName(inquiry.getCoapplicantMiddleName());
        existing.setCoapplicantFatherHusbandName(inquiry.getCoapplicantFatherHusbandName());
        existing.setCoapplicantDob(inquiry.getCoapplicantDob());
        existing.setCoapplicantGender(inquiry.getCoapplicantGender());
        existing.setCoapplicantMaritalStatus(inquiry.getCoapplicantMaritalStatus());
        existing.setCoapplicantResidencyStatus(inquiry.getCoapplicantResidencyStatus());
        existing.setCoapplicantCategory(inquiry.getCoapplicantCategory());
        existing.setCoapplicantEducation(inquiry.getCoapplicantEducation());
        existing.setCoapplicantOccupation(inquiry.getCoapplicantOccupation());
        existing.setCoapplicantEmployerName(inquiry.getCoapplicantEmployerName());
        existing.setCoapplicantGrossAnnualIncome(inquiry.getCoapplicantGrossAnnualIncome());
        existing.setCoapplicantNetAnnualIncome(inquiry.getCoapplicantNetAnnualIncome());
        existing.setCoapplicantRelation(inquiry.getCoapplicantRelation());

        // LIC HFL property
        existing.setPropertyFlatNo(inquiry.getPropertyFlatNo());
        existing.setPropertyFloorNo(inquiry.getPropertyFloorNo());
        existing.setPropertyBuilding(inquiry.getPropertyBuilding());
        existing.setPropertyArea(inquiry.getPropertyArea());
        existing.setPropertyLandmark(inquiry.getPropertyLandmark());
        existing.setPropertyCity(inquiry.getPropertyCity());
        existing.setPropertyState(inquiry.getPropertyState());
        existing.setPropertyPin(inquiry.getPropertyPin());
        existing.setPropertyOwnerName(inquiry.getPropertyOwnerName());
        existing.setPropertyValue(inquiry.getPropertyValue());

        // LIC HFL bank account
        existing.setBankAccountNo(inquiry.getBankAccountNo());
        existing.setBankIfsc(inquiry.getBankIfsc());
        existing.setBankAccountType(inquiry.getBankAccountType());
        existing.setBankAccountHolder(inquiry.getBankAccountHolder());

        // LIC HFL references
        existing.setRef1Name(inquiry.getRef1Name());
        existing.setRef1Address(inquiry.getRef1Address());
        existing.setRef1Mobile(inquiry.getRef1Mobile());
        existing.setRef1Email(inquiry.getRef1Email());
        existing.setRef1Relation(inquiry.getRef1Relation());
        existing.setRef2Name(inquiry.getRef2Name());
        existing.setRef2Address(inquiry.getRef2Address());
        existing.setRef2Mobile(inquiry.getRef2Mobile());
        existing.setRef2Email(inquiry.getRef2Email());
        existing.setRef2Relation(inquiry.getRef2Relation());

        // Insurance fields
        existing.setInsuranceType(inquiry.getInsuranceType());
        existing.setInsuranceSubType(inquiry.getInsuranceSubType());

        inquiryService.save(existing);
        redirectAttributes.addFlashAttribute("successMsg", "Inquiry updated successfully!");
        return "redirect:/inquiries/" + id;
    }
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        inquiryService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Inquiry deleted.");
        return "redirect:/inquiries";
    }
    
    @PostMapping("/{id}/quick-status")
    public String quickStatus(@PathVariable Long id,
                              @RequestParam String status,
                              @RequestParam(required = false) String redirectUrl,
                              RedirectAttributes redirectAttributes) {
        Inquiry existing = inquiryService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inquiry", id));
        try {
            Inquiry.Status newStatus = Inquiry.Status.valueOf(status.toUpperCase());
            existing.setStatus(newStatus);
            if (newStatus == Inquiry.Status.APPROVED || newStatus == Inquiry.Status.REJECTED) {
                existing.setActualCloseDate(java.time.LocalDate.now());
            }
            inquiryService.save(existing);
            redirectAttributes.addFlashAttribute("successMsg", "Status updated to " + status.replace("_", " ") + "!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Invalid status: " + status);
        }
        return "redirect:" + (redirectUrl != null && !redirectUrl.isBlank() ? redirectUrl : "/inquiries");
    }

    private void populateFormModel(Model model) {
        model.addAttribute("inquiryTypes", inquiryTypeRepository.findByIsActiveTrueOrderBySortOrderAsc());
        model.addAttribute("statuses",     Inquiry.Status.values());
        model.addAttribute("priorities",   Inquiry.Priority.values());
        model.addAttribute("allUsers",     userService.findAll(PageRequest.of(0, 100)).getContent());
        model.addAttribute("today",        LocalDate.now());
    }
}