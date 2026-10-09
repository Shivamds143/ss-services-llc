package com.financialcrm.controller;

import com.financialcrm.entity.Customer;
import com.financialcrm.entity.User;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.service.CustomerService;
import com.financialcrm.service.InquiryService;
import com.financialcrm.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/customers")
@Slf4j
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final InquiryService inquiryService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search,
                       Model model) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Customer> customers = customerService.search(search, pageable);

        model.addAttribute("customers", customers);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customers.getTotalPages());
        model.addAttribute("totalElements", customers.getTotalElements());
        return "customer/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("customer", new Customer());
        model.addAttribute("today", LocalDate.now());
        return "customer/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Customer customer,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("today", LocalDate.now());
            return "customer/form";
        }
        User currentUser = securityUtils.getCurrentUserOrThrow();
        customer.setCreatedBy(currentUser);
        if (customer.getInquiryDate() == null) customer.setInquiryDate(LocalDate.now());

        customerService.save(customer);
        redirectAttributes.addFlashAttribute("successMsg",
            "Customer " + customer.getCustomerName() + " saved successfully!");
        return "redirect:/customers";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        model.addAttribute("customer", customer);
        model.addAttribute("inquiries", inquiryService.findByCustomerId(id));
        return "customer/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        model.addAttribute("customer", customer);
        model.addAttribute("today", LocalDate.now());
        return "customer/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute Customer customer,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("today", LocalDate.now());
            return "customer/form";
        }
        Customer existing = customerService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        existing.setCustomerName(customer.getCustomerName());
        existing.setMobileNumber(customer.getMobileNumber());
        existing.setAlternateMobile(customer.getAlternateMobile());
        existing.setEmail(customer.getEmail());
        existing.setReferenceName(customer.getReferenceName());
        existing.setAddress(customer.getAddress());
        existing.setCity(customer.getCity());
        existing.setState(customer.getState());
        existing.setPincode(customer.getPincode());
        existing.setDateOfBirth(customer.getDateOfBirth());
        existing.setAnniversaryDate(customer.getAnniversaryDate());
        existing.setNotes(customer.getNotes());
        existing.setInquiryDate(customer.getInquiryDate());

        customerService.save(existing);
        redirectAttributes.addFlashAttribute("successMsg", "Customer updated successfully!");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        customerService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Customer deleted successfully.");
        return "redirect:/customers";
    }
}
