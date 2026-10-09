package com.financialcrm.controller;

import com.financialcrm.entity.Customer;
import com.financialcrm.service.CustomerService;
import com.financialcrm.service.NotificationService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final CustomerService customerService;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    /**
     * Customer live search endpoint used by inquiry form autocomplete
     */
    @GetMapping("/customers/search")
    public ResponseEntity<List<Map<String, Object>>> searchCustomers(
            @RequestParam(defaultValue = "") String q) {
        var customers = customerService.search(q, PageRequest.of(0, 10));
        var result = customers.getContent().stream()
            .map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "customerName", c.getCustomerName(),
                "mobileNumber", c.getMobileNumber(),
                "city", c.getCity() != null ? c.getCity() : ""
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Get unread notification count for current user
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Long count = securityUtils.getCurrentUser()
            .map(u -> notificationService.countUnread(u.getId()))
            .orElse(0L);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark single notification as read
     */
    @PostMapping("/notifications/mark-read/{id}")
    public ResponseEntity<Map<String, Object>> markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        Long remaining = securityUtils.getCurrentUser()
            .map(u -> notificationService.countUnread(u.getId()))
            .orElse(0L);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", remaining));
    }

    /**
     * Dashboard quick stats for AJAX refresh
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> dashboardStats() {
        var stats = Map.<String, Object>of(
            "totalLeads", customerService.countActiveCustomers(),
            "todayVisitors", customerService.countTodayVisitors()
        );
        return ResponseEntity.ok(stats);
    }
}
