package com.financialcrm.controller;

import com.financialcrm.entity.*;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.service.*;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/followups")
@RequiredArgsConstructor
public class FollowupController {

    private final FollowupService followupService;
    private final InquiryService inquiryService;
    private final CustomerService customerService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("todayFollowups",    followupService.findTodayFollowups());
        model.addAttribute("overdueFollowups",  followupService.findOverdueFollowups());
        model.addAttribute("upcomingFollowups", followupService.findUpcomingFollowups(7));
        model.addAttribute("todayCount",        followupService.countTodayPending());
        model.addAttribute("currentDate",       LocalDate.now());
        return "followup/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam Long inquiryId, Model model) {
        Inquiry inquiry = inquiryService.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", inquiryId));

        // Guard: inquiry must have a customer linked
        if (inquiry.getCustomer() == null) {
            throw new IllegalStateException("Inquiry #" + inquiryId + " has no customer linked. Please edit the inquiry and set a customer first.");
        }

        Followup followup = new Followup();
        followup.setInquiry(inquiry);
        followup.setCustomer(inquiry.getCustomer());
        followup.setFollowupDate(LocalDate.now());
        followup.setAutoReminderEnabled(true);
        followup.setReminderDaysBefore(0);
        followup.setOutcome(Followup.Outcome.NEUTRAL);
        followup.setStatus(Followup.Status.PENDING);

        model.addAttribute("followup", followup);
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("outcomes", Followup.Outcome.values());
        return "followup/form";
    }

    // FIX: the edit form posts to "/followups/{id}/save" when updating an existing
    // follow-up (see followup/form.html), but previously only "/followups/save"
    // (no id) was mapped here. That meant every edit-and-save from
    // /followups/{id}/edit hit no matching handler (404) and nothing was ever
    // updated. This single mapping now handles BOTH create ("/followups/save")
    // and update ("/followups/{id}/save"), loading the existing row by id so we
    // don't lose fields that aren't present on the form (createdAt, createdBy,
    // callReminderSent, whatsappReminderSent, etc).
    @PostMapping({"/save", "/{id}/save"})
    public String save(@PathVariable(required = false) Long id,
                       @ModelAttribute Followup formFollowup,
                       @RequestParam Long inquiryId,
                       @RequestParam Long customerId,
                       @RequestParam(required = false) String followupTimeStr,
                       @RequestParam(required = false) String nextFollowupTimeStr,
                       @RequestParam(required = false) String callReminderTimeStr,
                       @RequestParam(required = false) String whatsappReminderTimeStr,
                       RedirectAttributes redirectAttributes) {

        Inquiry inquiry = inquiryService.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", inquiryId));
        Customer customer = customerService.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        User user = securityUtils.getCurrentUserOrThrow();

        Followup followup;
        if (id != null) {
            // Updating an existing follow-up
            followup = followupService.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Followup", id));
        } else {
            // Creating a brand new follow-up
            followup = new Followup();
            followup.setCreatedBy(user);
        }

        followup.setInquiry(inquiry);
        followup.setCustomer(customer);

        // Copy over the editable fields submitted from the form
        followup.setFollowupDate(formFollowup.getFollowupDate());
        followup.setNextFollowupDate(formFollowup.getNextFollowupDate());
        followup.setNextFollowupReason(formFollowup.getNextFollowupReason());
        followup.setCallReminder(Boolean.TRUE.equals(formFollowup.getCallReminder()));
        followup.setWhatsappReminder(Boolean.TRUE.equals(formFollowup.getWhatsappReminder()));
        followup.setWhatsappMessageTemplate(formFollowup.getWhatsappMessageTemplate());
        followup.setFollowupNotes(formFollowup.getFollowupNotes());
        followup.setInternalNotes(formFollowup.getInternalNotes());
        followup.setCustomerFeedback(formFollowup.getCustomerFeedback());
        followup.setAutoReminderEnabled(
                formFollowup.getAutoReminderEnabled() != null ? formFollowup.getAutoReminderEnabled() : Boolean.TRUE);
        followup.setReminderDaysBefore(formFollowup.getReminderDaysBefore());
        followup.setOutcome(formFollowup.getOutcome() != null ? formFollowup.getOutcome() : Followup.Outcome.NEUTRAL);

        // Default status if not already set (only relevant for new records)
        if (followup.getStatus() == null) followup.setStatus(Followup.Status.PENDING);

        followup.setFollowupTime(
                (followupTimeStr != null && !followupTimeStr.isEmpty()) ? LocalTime.parse(followupTimeStr) : null);
        followup.setNextFollowupTime(
                (nextFollowupTimeStr != null && !nextFollowupTimeStr.isEmpty()) ? LocalTime.parse(nextFollowupTimeStr) : null);
        followup.setCallReminderTime(
                (callReminderTimeStr != null && !callReminderTimeStr.isEmpty()) ? LocalTime.parse(callReminderTimeStr) : null);
        followup.setWhatsappReminderTime(
                (whatsappReminderTimeStr != null && !whatsappReminderTimeStr.isEmpty()) ? LocalTime.parse(whatsappReminderTimeStr) : null);

        if (Boolean.TRUE.equals(followup.getCallReminder())
                || Boolean.TRUE.equals(followup.getWhatsappReminder())) {
            followup.setAutoReminderEnabled(true);
        }

        followupService.save(followup);

        if (inquiry.getStatus() == Inquiry.Status.NEW) {
            inquiry.setStatus(Inquiry.Status.FOLLOW_UP);
            inquiryService.save(inquiry);
        }
        redirectAttributes.addFlashAttribute("successMsg",
                id != null ? "Follow-up updated successfully!" : "Follow-up saved successfully!");
        return "redirect:/inquiries/" + inquiryId;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Followup followup = followupService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Followup", id));
        // Re-load inquiry with full details for template
        Inquiry inquiry = inquiryService.findById(followup.getInquiry().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", followup.getInquiry().getId()));
        model.addAttribute("followup", followup);
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("statuses", Followup.Status.values());
        model.addAttribute("outcomes", Followup.Outcome.values());
        return "followup/form";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                           @RequestParam String notes,
                           @RequestParam(required = false) String internalNotes,
                           @RequestParam(required = false) String customerFeedback,
                           @RequestParam(required = false) String outcome,
                           @RequestParam(required = false) LocalDate nextFollowupDate,
                           @RequestParam(required = false) String nextFollowupReason,
                           RedirectAttributes redirectAttributes) {
        Followup followup = followupService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Followup", id));
        followup.setStatus(Followup.Status.COMPLETED);
        followup.setFollowupNotes(notes);
        if (internalNotes != null && !internalNotes.isEmpty())
            followup.setInternalNotes(internalNotes);
        if (customerFeedback != null && !customerFeedback.isEmpty())
            followup.setCustomerFeedback(customerFeedback);
        if (outcome != null && !outcome.isEmpty())
            followup.setOutcome(Followup.Outcome.valueOf(outcome));
        if (nextFollowupDate != null) {
            followup.setNextFollowupDate(nextFollowupDate);
            followup.setNextFollowupReason(nextFollowupReason);
        }
        followupService.save(followup);
        redirectAttributes.addFlashAttribute("successMsg", "Follow-up marked as completed!");
        return "redirect:/followups";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        followupService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Follow-up deleted.");
        return "redirect:/followups";
    }

    @PostMapping("/send-reminders")
    public String triggerReminders(RedirectAttributes redirectAttributes) {
        int count = followupService.sendAutoReminders();
        redirectAttributes.addFlashAttribute("successMsg", count + " reminders sent successfully!");
        return "redirect:/followups";
    }
}