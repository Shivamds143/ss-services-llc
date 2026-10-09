package com.financialcrm.controller;

import com.financialcrm.service.WhatsAppSchedulerService;
import com.financialcrm.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppSchedulerService whatsAppSchedulerService;

    /**
     * WhatsApp settings/dashboard page
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String whatsAppDashboard(Model model) {
        return "whatsapp/dashboard";
    }

    /**
     * Manually trigger today's birthday messages (Admin only)
     */
    @PostMapping("/send-birthday-wishes")
    @PreAuthorize("hasRole('ADMIN')")
    public String triggerBirthdayWishes(RedirectAttributes redirectAttributes) {
        try {
            whatsAppSchedulerService.sendBirthdayWishes();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Birthday wishes sent successfully to all customers with birthday today!");
        } catch (Exception e) {
            log.error("Error triggering birthday wishes: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error sending birthday wishes: " + e.getMessage());
        }
        return "redirect:/whatsapp";
    }

    /**
     * Manually trigger today's anniversary messages (Admin only)
     */
    @PostMapping("/send-anniversary-wishes")
    @PreAuthorize("hasRole('ADMIN')")
    public String triggerAnniversaryWishes(RedirectAttributes redirectAttributes) {
        try {
            whatsAppSchedulerService.sendAnniversaryWishes();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Anniversary wishes sent successfully to all customers with anniversary today!");
        } catch (Exception e) {
            log.error("Error triggering anniversary wishes: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error sending anniversary wishes: " + e.getMessage());
        }
        return "redirect:/whatsapp";
    }

    /**
     * REST API: Send a test WhatsApp message to a specific number
     */
    @PostMapping("/api/test")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendTestMessage(
            @RequestParam String mobileNumber,
            @RequestParam String message) {

        Map<String, Object> response = new HashMap<>();
        boolean success = whatsAppService.sendMessage(mobileNumber, message);
        response.put("success", success);
        response.put("message", success
                ? "WhatsApp message sent successfully to " + mobileNumber
                : "Failed to send WhatsApp message to " + mobileNumber);
        return ResponseEntity.ok(response);
    }
}