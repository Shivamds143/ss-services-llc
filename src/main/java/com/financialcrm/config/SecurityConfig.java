package com.financialcrm.config;

import com.financialcrm.service.AuditService;
import com.financialcrm.service.impl.CustomUserDetailsService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;

    // FIX: secret key used to sign the remember-me cookie. In production set this
    // via app.security.remember-me-key in application.properties / env var.
    @Value("${app.security.remember-me-key:financialCrmRememberMeSecretKey}")
    private String rememberMeKey;

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // FIX: in-memory persistent token repository for remember-me.
    // No new DB table required. Tokens are lost on app restart, which is
    // acceptable for this use case (user just has to log in again after a
    // server restart, same as before).
    @Bean
    public PersistentTokenRepository tokenRepository() {
        return new InMemoryTokenRepositoryImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/forgot-password", "/auth/reset-password").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/utility-bills/*/phonepe/**").permitAll()
                .requestMatchers("/uploads/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "RECEPTIONIST", "EMPLOYEE")
                .requestMatchers("/income-dashboard").authenticated()
                .requestMatchers("/income-entries/**").authenticated()
                .requestMatchers("/income-categories/**").authenticated()
                .requestMatchers("/income-reports/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN", "remember-me")
                .permitAll()
            )
            // Use the standard DSL explicitly so the checkbox parameter,
            // cookie name, user details lookup, token storage, and validity
            // period are all aligned in one place.
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
                .rememberMeCookieName("remember-me")
                .tokenRepository(tokenRepository())
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(14 * 24 * 60 * 60)
                .alwaysRemember(false)
                .useSecureCookie(false)
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    "/api/notifications/mark-read/**",
                    "/notifications/mark-read/**",
                    "/calculator/**",
                    "/utility-bills/*/phonepe/callback"
                )
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/auth/login?expired")
            )
            .headers(headers -> headers
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    // FIX: dashboard charts were blank because Chart.js is loaded from
                    // cdn.jsdelivr.net in dashboard/index.html, but the CSP only
                    // whitelisted cdnjs.cloudflare.com. The browser silently blocked
                    // the script under CSP, so window.Chart was undefined and both
                    // canvases rendered empty. Added jsdelivr to script-src.
                    "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; " +
                    "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                    "img-src 'self' data:;"
                ))
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        // FIX: this is the other reason the Audit Logs page looked basically
        // empty/IP-less — nothing ever wrote an entry on login, which is the
        // most common real-world audit event. Every successful login is now
        // logged with the real client IP (via SecurityUtils.getClientIp).
        return (request, response, authentication) -> {
            auditService.log(authentication.getName(), "LOGIN_SUCCESS", "User", null,
                    null, null, SecurityUtils.getClientIp(request));
            response.sendRedirect("/dashboard");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            System.out.println("================================");
            System.out.println("LOGIN FAILED");
            System.out.println("Exception : " + exception.getClass().getName());
            System.out.println("Message   : " + exception.getMessage());
            System.out.println("================================");
            // FIX: log failed login attempts too, with the real client IP —
            // useful for spotting brute-force / credential-stuffing attempts,
            // which is one of the main reasons audit logs capture IPs at all.
            String attemptedUsername = request.getParameter("username");
            auditService.log(attemptedUsername, "LOGIN_FAILURE", "User", null,
                    null, exception.getMessage(), SecurityUtils.getClientIp(request),
                    com.financialcrm.entity.AuditLog.Status.FAILURE);
            response.sendRedirect("/auth/login?error");
        };
    }
}
