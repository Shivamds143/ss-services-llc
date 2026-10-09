package com.financialcrm.service;

import com.financialcrm.entity.User;
import com.financialcrm.repository.UserRepository;
import com.financialcrm.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$12$encodedPassword");
        testUser.setFullName("Test User");
        testUser.setIsActive(true);
        testUser.setIsLocked(false);
        testUser.setFailedAttempts(0);
    }

    @Test
    void save_NewUser_ShouldEncodePassword() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("plainPassword");

        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$12$encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        User saved = userService.save(newUser);

        verify(passwordEncoder).encode("plainPassword");
        assertThat(saved.getPassword()).isEqualTo("$2a$12$encodedPwd");
    }

    @Test
    void save_ExistingUser_ShouldNotEncodePassword() {
        // Existing user (id != null) should NOT re-encode password
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.save(testUser);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void findById_ShouldReturnUser_WhenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void findByUsername_ShouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertThat(result).isPresent();
    }

    @Test
    void deleteById_ShouldDeactivateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.deleteById(1L);

        assertThat(testUser.getIsActive()).isFalse();
        verify(userRepository).save(testUser);
        verify(userRepository, never()).deleteById(1L);
    }

    @Test
    void changePassword_WithCorrectCurrentPassword_ShouldChangePassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPwd", "$2a$12$encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPwd")).thenReturn("$2a$12$newEncoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThatNoException().isThrownBy(() ->
            userService.changePassword("testuser", "currentPwd", "newPwd"));

        verify(passwordEncoder).encode("newPwd");
    }

    @Test
    void changePassword_WithWrongCurrentPassword_ShouldThrowException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPwd", "$2a$12$encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("testuser", "wrongPwd", "newPwd"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void initiatePasswordReset_ShouldSetTokenAndExpiry() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.initiatePasswordReset("test@example.com");

        assertThat(testUser.getPasswordResetToken()).isNotNull();
        assertThat(testUser.getPasswordResetExpiry()).isAfter(LocalDateTime.now());
        verify(userRepository).save(testUser);
    }

    @Test
    void resetPassword_WithValidToken_ShouldResetPassword() {
        testUser.setPasswordResetToken("valid-token");
        testUser.setPasswordResetExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken("valid-token")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("$2a$12$newEncoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.resetPassword("valid-token", "newPassword");

        assertThat(testUser.getPasswordResetToken()).isNull();
        assertThat(testUser.getPasswordResetExpiry()).isNull();
        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowException() {
        testUser.setPasswordResetToken("expired-token");
        testUser.setPasswordResetExpiry(LocalDateTime.now().minusHours(1)); // already expired

        when(userRepository.findByPasswordResetToken("expired-token")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.resetPassword("expired-token", "newPassword"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThat(userService.existsByUsername("testuser")).isTrue();
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenNotExists() {
        when(userRepository.existsByEmail("noemail@test.com")).thenReturn(false);

        assertThat(userService.existsByEmail("noemail@test.com")).isFalse();
    }
}
