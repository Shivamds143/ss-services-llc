package com.financialcrm.util;

import com.financialcrm.entity.User;
import com.financialcrm.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return Optional.empty();
        }
        return userRepository.findByUsername(auth.getName());
    }

    public User getCurrentUserOrThrow() {
        return getCurrentUser()
            .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role) || a.getAuthority().equals(role));
    }

    public boolean isAdmin() { return hasRole("ADMIN"); }
    public boolean isReceptionist() { return hasRole("RECEPTIONIST"); }

    /**
     * FIX: Audit log entries always showed a blank IP Address column because
     * the only place that called AuditService.log(...) hardcoded "" for the
     * ipAddress argument instead of reading it off the request. This helper
     * centralizes correct IP extraction (including the X-Forwarded-For /
     * X-Real-IP headers used when the app sits behind a reverse proxy or
     * load balancer, in which case request.getRemoteAddr() would otherwise
     * just return the proxy's own address) so every caller gets the real
     * client IP.
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "";
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For can contain a comma-separated chain; the first
            // entry is the original client.
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
