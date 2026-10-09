package com.financialcrm.controller;

import com.financialcrm.service.NotificationService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("notifications",
            notificationService.findAll(user.getId(), PageRequest.of(page, 20)));
        model.addAttribute("unreadCount", notificationService.countUnread(user.getId()));
        return "notification/list";
    }

    @PostMapping("/mark-all-read")
    public String markAllRead() {
        securityUtils.getCurrentUser().ifPresent(u -> notificationService.markAllAsRead(u.getId()));
        return "redirect:/notifications";
    }

    @PostMapping("/mark-read/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        Long unread = securityUtils.getCurrentUser()
            .map(u -> notificationService.countUnread(u.getId())).orElse(0L);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", unread));
    }
}
