package com.financialcrm.controller;

import com.financialcrm.entity.Role;
import com.financialcrm.entity.User;
import com.financialcrm.exception.ResourceNotFoundException;
import com.financialcrm.repository.RoleRepository;
import com.financialcrm.repository.UserRepository;
import com.financialcrm.service.AuditService;
import com.financialcrm.service.UserService;
import com.financialcrm.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    // PasswordEncoder removed - using NoOpPasswordEncoder via UserService

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Model model) {
        Page<User> users = userService.findAll(PageRequest.of(page, size, Sort.by("fullName")));
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute User user,
                           @RequestParam List<Long> roleIds,
                           @RequestParam String rawPassword,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {

        if (userService.existsByUsername(user.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Username already exists.");
            return "redirect:/admin/users/new";
        }
        if (userService.existsByEmail(user.getEmail())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Email already exists.");
            return "redirect:/admin/users/new";
        }

        Set<Role> roles = roleIds.stream()
            .map(id -> roleRepository.findById(id).orElse(null))
            .filter(r -> r != null)
            .collect(Collectors.toSet());

        user.setRoles(roles);
        user.setPassword(rawPassword);
        user.setIsActive(true);

        // FIX: use userRepository.saveAndFlush directly to avoid cascade persist issue
        User saved = userRepository.saveAndFlush(user);

        // FIX: this previously hardcoded the acting username as "admin" and the
        // IP address as "" (empty string), which is why every row on the Audit
        // Logs page showed a blank IP Address column. Now uses the actual
        // logged-in admin's username and the real client IP (via
        // SecurityUtils.getClientIp, which also understands X-Forwarded-For
        // when running behind a proxy/load balancer).
        String actingUsername = securityUtils.getCurrentUser()
                .map(User::getUsername)
                .orElse("admin");
        auditService.log(actingUsername, "CREATE_USER", "User", saved.getId(), null,
                saved.getUsername(), SecurityUtils.getClientIp(request));
        redirectAttributes.addFlashAttribute("successMsg", "User created successfully!");
        return "redirect:/admin/users";
    }
    
    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    @PostMapping("/users/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.findById(id).ifPresent(u -> {
            u.setIsActive(!u.getIsActive());
            userRepository.save(u);
        });
        redirectAttributes.addFlashAttribute("successMsg", "User status updated.");
        return "redirect:/admin/users";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(defaultValue = "0") int page, Model model) {
        var logs = auditService.findAll(PageRequest.of(page, 50));
        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logs.getTotalPages());
        return "admin/audit-logs";
    }
}