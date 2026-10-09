package com.financialcrm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.financialcrm.service.EmailSchedulerService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailSchedulerService emailSchedulerService;

    @PostMapping("/send-birthday-now")
    public String sendBirthdayNow(Model model) {
        emailSchedulerService.sendBirthdayEmails();
        model.addAttribute("emailMessage", "Birthday emails triggered successfully. Check logs for details.");
        return "redirect:/dashboard?emailMessage=Birthday emails triggered successfully";
    }

    @PostMapping("/send-anniversary-now")
    public String sendAnniversaryNow(Model model) {
        emailSchedulerService.sendAnniversaryEmails();
        model.addAttribute("emailMessage", "Anniversary emails triggered successfully. Check logs for details.");
        return "redirect:/dashboard?emailMessage=Anniversary emails triggered successfully";
    }
    
}