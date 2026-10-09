package com.financialcrm.service.impl;

import com.financialcrm.entity.*;
import com.financialcrm.repository.*;
import com.financialcrm.service.FollowupService;
import com.financialcrm.service.NotificationService;
import com.financialcrm.service.UtilityBillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {

    private final FollowupService followupService;       // ← replaces followupRepository for reminders
    private final FollowupRepository followupRepository; // ← still needed for overdue alerts
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UtilityBillService utilityBillService;

    /**
     * Run every day at 8:00 AM — send all followup auto-reminders
     * (same-day, advance, next-followup-date)
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendFollowupReminders() {
        log.info("Running daily followup auto-reminders...");
        int count = followupService.sendAutoReminders();
        log.info("Auto reminders completed. Total sent: {}", count);
    }

    /**
     * Run every day at 7:00 AM — birthday reminders
     */
    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional
    public void sendBirthdayReminders() {
        log.info("Running birthday reminders...");
        List<Customer> birthdays = customerRepository.findTodayBirthdays();
        if (birthdays.isEmpty()) return;

        // Notify all admin/receptionist users
        List<User> staff = userRepository.findAll().stream()
            .filter(u -> u.getIsActive() &&
                u.getRoles().stream().anyMatch(r ->
                    r.getRoleName().equals("ROLE_ADMIN") || r.getRoleName().equals("ROLE_RECEPTIONIST")))
            .toList();

        for (Customer c : birthdays) {
            String msg = "🎂 Today is " + c.getCustomerName() + "'s birthday! Mobile: " + c.getMobileNumber();
            for (User u : staff) {
                try {
                    notificationService.createNotification(
                        u.getId(), "Birthday Reminder", msg,
                        Notification.NotificationType.BIRTHDAY_REMINDER
                    );
                } catch (Exception e) {
                    log.error("Error sending birthday notification", e);
                }
            }
        }
        log.info("Sent birthday reminders for {} customers.", birthdays.size());
    }

    /**
     * Run every day at 8:30 AM — overdue followup alerts
     */
    @Scheduled(cron = "0 30 8 * * ?")
    @Transactional
    public void alertOverdueFollowups() {
        log.info("Checking overdue followups...");
        List<Followup> overdue = followupRepository.findOverdueFollowups(LocalDate.now());
        for (Followup f : overdue) {
            try {
                Long userId = f.getCreatedBy().getId();
                notificationService.createNotification(
                    userId,
                    "Overdue Follow-up Alert",
                    "Follow-up for " + f.getCustomer().getCustomerName() +
                        " was due on " + f.getFollowupDate() + " and is still pending.",
                    Notification.NotificationType.FOLLOWUP_REMINDER
                );
            } catch (Exception e) {
                log.error("Error sending overdue followup alert", e);
            }
        }
    }

    /**
     * Run every day at 9:00 AM — utility bill due reminders + overdue alerts
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendUtilityBillReminders() {
        log.info("Checking utility bill reminders...");

        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getIsActive() &&
                        u.getRoles().stream().anyMatch(r -> r.getRoleName().equals("ROLE_ADMIN")))
                .toList();

        if (admins.isEmpty()) {
            log.warn("No active ADMIN users found — skipping utility bill notifications.");
            return;
        }

        // Upcoming bills due within reminderDaysBefore
        for (UtilityBill bill : utilityBillService.findDueForReminder(7)) {
            long daysLeft = LocalDate.now().until(bill.getDueDate()).getDays();
            if (daysLeft <= bill.getReminderDaysBefore()) {
                String msg = "Bill '" + bill.getBillName() + "' (₹" + bill.getAmount() +
                        ") is due on " + bill.getDueDate() + ".";
                for (User u : admins) {
                    try {
                        notificationService.createNotification(
                                u.getId(), "Utility Bill Due", msg,
                                Notification.NotificationType.EMI_DUE
                        );
                    } catch (Exception e) {
                        log.error("Error sending utility bill due notification", e);
                    }
                }
            }
        }

        // Overdue bills
        for (UtilityBill bill : utilityBillService.findOverdue()) {
            String msg = "Bill '" + bill.getBillName() + "' (₹" + bill.getAmount() +
                    ") was due on " + bill.getDueDate() + " and is still unpaid!";
            for (User u : admins) {
                try {
                    notificationService.createNotification(
                            u.getId(), "Overdue Utility Bill", msg,
                            Notification.NotificationType.EMI_DUE
                    );
                } catch (Exception e) {
                    log.error("Error sending overdue utility bill notification", e);
                }
            }
        }

        log.info("Utility bill reminder check complete.");
    }
}