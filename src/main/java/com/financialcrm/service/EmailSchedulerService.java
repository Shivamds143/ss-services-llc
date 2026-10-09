package com.financialcrm.service;

import com.financialcrm.entity.Customer;
import com.financialcrm.entity.EmailLog;
import com.financialcrm.repository.CustomerRepository;
import com.financialcrm.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSchedulerService {

    private final CustomerRepository customerRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${app.email.birthday.cron}", zone = "Asia/Kolkata")
    public void sendBirthdayEmails() {
        log.info("=== Email Birthday Scheduler Started ===");

        List<Customer> birthdayCustomers = customerRepository.findTodayBirthdays();
        log.info("Found {} customer(s) with birthday today", birthdayCustomers.size());

        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        for (Customer customer : birthdayCustomers) {
            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                log.warn("Skipping customer {} - no email address", customer.getCustomerName());
                continue;
            }

            if (alreadySentToday(customer.getId(), EmailLog.EmailType.BIRTHDAY)) {
                log.info("Skipping customer {} - birthday email already sent today", customer.getCustomerName());
                skippedCount++;
                continue;
            }

            boolean sent = emailService.sendBirthdayEmail(customer.getCustomerName(), customer.getEmail());
            saveEmailLog(customer, EmailLog.EmailType.BIRTHDAY, sent);

            if (sent) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("=== Birthday Email Scheduler Done | Success: {} | Failed: {} | Skipped(duplicate): {} ===",
                successCount, failCount, skippedCount);
    }

    @Scheduled(cron = "${app.email.anniversary.cron}", zone = "Asia/Kolkata")
    public void sendAnniversaryEmails() {
        log.info("=== Email Anniversary Scheduler Started ===");

        List<Customer> anniversaryCustomers = customerRepository.findTodayAnniversaries();
        log.info("Found {} customer(s) with anniversary today", anniversaryCustomers.size());

        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        for (Customer customer : anniversaryCustomers) {
            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                log.warn("Skipping customer {} - no email address", customer.getCustomerName());
                continue;
            }

            if (alreadySentToday(customer.getId(), EmailLog.EmailType.ANNIVERSARY)) {
                log.info("Skipping customer {} - anniversary email already sent today", customer.getCustomerName());
                skippedCount++;
                continue;
            }

            boolean sent = emailService.sendAnniversaryEmail(customer.getCustomerName(), customer.getEmail());
            saveEmailLog(customer, EmailLog.EmailType.ANNIVERSARY, sent);

            if (sent) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("=== Anniversary Email Scheduler Done | Success: {} | Failed: {} | Skipped(duplicate): {} ===",
                successCount, failCount, skippedCount);
    }

    private boolean alreadySentToday(Long customerId, EmailLog.EmailType type) {
        return emailLogRepository.existsByCustomerIdAndEmailTypeAndSentDate(
                customerId, type, LocalDate.now());
    }

    private void saveEmailLog(Customer customer, EmailLog.EmailType type, boolean sent) {
        EmailLog emailLog = EmailLog.builder()
                .customer(customer)
                .emailType(type)
                .emailAddress(customer.getEmail())
                .sentDate(LocalDate.now())
                .status(sent ? EmailLog.EmailStatus.SUCCESS : EmailLog.EmailStatus.FAILED)
                .build();
        emailLogRepository.save(emailLog);
    }
}