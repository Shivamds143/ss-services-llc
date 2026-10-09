package com.financialcrm.service;

import com.financialcrm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Page<User> findAll(Pageable pageable);
    void deleteById(Long id);
    void changePassword(String username, String currentPassword, String newPassword);
    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
