package com.financialcrm.service;

import com.financialcrm.entity.Customer;
import com.financialcrm.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppSchedulerService {

    private final CustomerRepository customerRepository;
    private final WhatsAppService whatsAppService;

    /**
     * Runs every day at 9:00 AM - sends birthday wishes to all customers
     * whose birthday is today.
     */
    @Scheduled(cron = "${app.whatsapp.birthday.cron}", zone = "Asia/Kolkata")
    public void sendBirthdayWishes() {
        log.info("=== WhatsApp Birthday Scheduler Started ===");

        List<Customer> birthdayCustomers = customerRepository.findTodayBirthdays();
        log.info("Found {} customer(s) with birthday today", birthdayCustomers.size());

        int successCount = 0;
        int failCount = 0;

        for (Customer customer : birthdayCustomers) {
            if (customer.getMobileNumber() == null || customer.getMobileNumber().isBlank()) {
                log.warn("Skipping customer {} - no mobile number", customer.getCustomerName());
                continue;
            }

            boolean sent = whatsAppService.sendBirthdayWishes(
                    customer.getCustomerName(),
                    customer.getMobileNumber()
            );

            if (sent) {
                successCount++;
            } else {
                failCount++;
                // Also try alternate number if primary fails
                if (customer.getAlternateMobile() != null && !customer.getAlternateMobile().isBlank()) {
                    log.info("Retrying with alternate number for {}", customer.getCustomerName());
                    whatsAppService.sendBirthdayWishes(
                            customer.getCustomerName(),
                            customer.getAlternateMobile()
                    );
                }
            }
        }

        log.info("=== Birthday WhatsApp Scheduler Done | Success: {} | Failed: {} ===",
                successCount, failCount);
    }

    /**
     * Runs every day at 9:00 AM - sends anniversary wishes to all customers
     * whose anniversary is today.
     */
    @Scheduled(cron = "${app.whatsapp.anniversary.cron}", zone = "Asia/Kolkata")
    public void sendAnniversaryWishes() {
        log.info("=== WhatsApp Anniversary Scheduler Started ===");

        List<Customer> anniversaryCustomers = customerRepository.findTodayAnniversaries();
        log.info("Found {} customer(s) with anniversary today", anniversaryCustomers.size());

        int successCount = 0;
        int failCount = 0;

        for (Customer customer : anniversaryCustomers) {
            if (customer.getMobileNumber() == null || customer.getMobileNumber().isBlank()) {
                log.warn("Skipping customer {} - no mobile number", customer.getCustomerName());
                continue;
            }

            boolean sent = whatsAppService.sendAnniversaryWishes(
                    customer.getCustomerName(),
                    customer.getMobileNumber()
            );

            if (sent) {
                successCount++;
            } else {
                failCount++;
                if (customer.getAlternateMobile() != null && !customer.getAlternateMobile().isBlank()) {
                    log.info("Retrying with alternate number for {}", customer.getCustomerName());
                    whatsAppService.sendAnniversaryWishes(
                            customer.getCustomerName(),
                            customer.getAlternateMobile()
                    );
                }
            }
        }

        log.info("=== Anniversary WhatsApp Scheduler Done | Success: {} | Failed: {} ===",
                successCount, failCount);
    }
}