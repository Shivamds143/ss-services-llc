package com.financialcrm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test that verifies the Spring application context loads successfully.
 * This is the most important test - if the context fails to start, all functionality is broken.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class FinancialCrmApplicationTests {

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void contextLoads() {
        // Verifies that the entire Spring application context initializes without errors.
        // This catches: missing beans, circular dependencies, misconfigured @Values,
        // JPA validation errors, and auto-configuration failures.
    }
}
