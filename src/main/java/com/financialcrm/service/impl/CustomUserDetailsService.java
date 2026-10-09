package com.financialcrm.service.impl;

import com.financialcrm.entity.User;
import com.financialcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UsernameNotFoundException(
                            "User not found: " + username);
                });

        log.info("Login attempt for user: {}", user.getUsername());

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.error("User account disabled: {}", username);
            throw new UsernameNotFoundException(
                    "User account is disabled");
        }

        List<GrantedAuthority> authorities;

        if (user.getRoles() == null || user.getRoles().isEmpty()) {

            log.warn("No roles assigned to user: {}",
                    user.getUsername());

            authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        } else {

            authorities = user.getRoles().stream()
                    .map(role -> {
                        log.info("Role found: {}",
                                role.getRoleName());

                        return new SimpleGrantedAuthority(
                                role.getRoleName());
                    })
                    .collect(Collectors.toList());
        }

        log.info("Authorities: {}", authorities);

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(Boolean.TRUE.equals(user.getIsLocked()))
                .credentialsExpired(false)
                .disabled(!Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }
}