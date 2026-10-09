package com.financialcrm.controller;

import com.financialcrm.entity.User;
import com.financialcrm.service.UserService;
import com.financialcrm.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "expired", required = false) String expired,
            Model model) {

        if (error != null) model.addAttribute("errorMsg", "Invalid username or password.");
        if (logout != null) model.addAttribute("successMsg", "You have been logged out successfully.");
        if (expired != null) model.addAttribute("errorMsg", "Your session has expired. Please login again.");
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam @Email @NotBlank String email,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.initiatePasswordReset(email);
            redirectAttributes.addFlashAttribute("successMsg",
                "If an account exists with that email, a password reset link has been sent.");
        } catch (Exception e) {
            log.error("Forgot password error", e);
            redirectAttributes.addFlashAttribute("successMsg",
                "If an account exists with that email, a password reset link has been sent.");
        }
        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                 @RequestParam @NotBlank @Size(min = 8) String newPassword,
                                 @RequestParam @NotBlank String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Passwords do not match.");
            return "redirect:/auth/reset-password?token=" + token;
        }
        try {
            userService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("successMsg", "Password reset successfully. Please login.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/auth/reset-password?token=" + token;
        }
    }

    @GetMapping("/change-password")
    public String changePasswordPage() {
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam @NotBlank String currentPassword,
                                  @RequestParam @NotBlank @Size(min = 8) String newPassword,
                                  @RequestParam @NotBlank String confirmPassword,
                                  RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMsg", "New passwords do not match.");
            return "redirect:/auth/change-password";
        }
        try {
            String username = securityUtils.getCurrentUsername();
            userService.changePassword(username, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMsg", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/auth/change-password";
    }
    
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute User user,
            RedirectAttributes redirectAttributes) {

        if(userService.existsByUsername(user.getUsername())) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    "Username already exists");
            return "redirect:/auth/register";
        }

        if(userService.existsByEmail(user.getEmail())) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    "Email already exists");
            return "redirect:/auth/register";
        }

        userService.save(user);

        redirectAttributes.addFlashAttribute(
                "successMsg",
                "Registration successful. Please login.");

        return "redirect:/auth/login";
    }
}
