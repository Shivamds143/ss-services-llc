package com.financialcrm.service.impl;

import com.financialcrm.entity.Followup;
import com.financialcrm.entity.Notification;
import com.financialcrm.repository.FollowupRepository;
import com.financialcrm.service.FollowupService;
import com.financialcrm.service.NotificationService;
import com.financialcrm.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FollowupServiceImpl implements FollowupService {

    private final FollowupRepository followupRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Override
    public Followup save(Followup followup) {
        if (followup.getId() == null) {
            followup.setCreatedBy(securityUtils.getCurrentUserOrThrow());
        }
        return followupRepository.save(followup);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Followup> findById(Long id) {
        return followupRepository.findByIdWithDetails(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Followup> findByInquiryId(Long inquiryId) {
        return followupRepository.findByInquiryIdOrderByFollowupDateDesc(inquiryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Followup> findTodayFollowups() {
        return followupRepository.findTodayFollowups(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Followup> findOverdueFollowups() {
        return followupRepository.findOverdueFollowups(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Followup> findUpcomingFollowups(int days) {
        return followupRepository.findUpcomingFollowups(LocalDate.now(),
                LocalDate.now().plusDays(days));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Followup> findByNextFollowupDate(LocalDate date) {
        return followupRepository.findByNextFollowupDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Followup> findAll(Pageable pageable) {
        return followupRepository.findAll(pageable);
    }

    @Override
    public void deleteById(Long id) {
        followupRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTodayPending() {
        return followupRepository.countByStatusAndFollowupDate(Followup.Status.PENDING, LocalDate.now());
    }

    // ── 5) AUTO REMINDER LOGIC ───────────────────────────────
    @Override
    public int sendAutoReminders() {
        int count = 0;
        LocalDate today = LocalDate.now();

        // Same-day reminders (call / whatsapp checked)
        List<Followup> sameDayList = followupRepository.findPendingReminders(today);
        for (Followup f : sameDayList) {
            try {
                Long userId = f.getAssignedTo() != null
                        ? f.getAssignedTo().getId()
                        : f.getCreatedBy().getId();

                String reminderTypes = buildReminderTypeLabel(f);
                notificationService.createNotification(
                        userId,
                        "Follow-up Reminder Today",
                        reminderTypes + " follow-up due today with "
                                + f.getCustomer().getCustomerName()
                                + " | " + f.getInquiry().getInquiryType().getTypeName(),
                        Notification.NotificationType.FOLLOWUP_REMINDER
                );
                f.setReminderSent(true);
                f.setReminderSentAt(LocalDateTime.now());
                followupRepository.save(f);
                count++;
            } catch (Exception e) {
                log.error("Error sending same-day reminder id={}", f.getId(), e);
            }
        }

        // Advance reminders (N days before followup date)
        List<Followup> advanceList = followupRepository.findAdvanceReminders(today).stream()
                .filter(f -> f.getReminderDaysBefore() != null)
                .filter(f -> today.plusDays(f.getReminderDaysBefore()).equals(f.getFollowupDate()))
                .collect(Collectors.toList());
        for (Followup f : advanceList) {
            try {
                Long userId = f.getAssignedTo() != null
                        ? f.getAssignedTo().getId()
                        : f.getCreatedBy().getId();

                notificationService.createNotification(
                        userId,
                        "Upcoming Follow-up Reminder",
                        "Follow-up with " + f.getCustomer().getCustomerName()
                                + " is scheduled on " + f.getFollowupDate()
                                + " (" + f.getReminderDaysBefore() + " day(s) advance notice)",
                        Notification.NotificationType.FOLLOWUP_REMINDER
                );
                f.setReminderSent(true);
                f.setReminderSentAt(LocalDateTime.now());
                followupRepository.save(f);
                count++;
            } catch (Exception e) {
                log.error("Error sending advance reminder id={}", f.getId(), e);
            }
        }

        // Next follow-up date reminders (from completed follow-ups)
        List<Followup> nextDateList = followupRepository.findByNextFollowupDate(today);
        for (Followup f : nextDateList) {
            try {
                Long userId = f.getCreatedBy().getId();
                notificationService.createNotification(
                        userId,
                        "Next Follow-up Due Today",
                        "Scheduled next follow-up with " + f.getCustomer().getCustomerName()
                                + " is today! Reason: " + (f.getNextFollowupReason() != null
                                ? f.getNextFollowupReason() : "Not specified"),
                        Notification.NotificationType.FOLLOWUP_REMINDER
                );
                count++;
            } catch (Exception e) {
                log.error("Error sending next-date reminder id={}", f.getId(), e);
            }
        }

        log.info("Auto reminders sent: {}", count);
        return count;
    }

    private String buildReminderTypeLabel(Followup f) {
        if (Boolean.TRUE.equals(f.getCallReminder()) && Boolean.TRUE.equals(f.getWhatsappReminder()))
            return "📞 Call + 💬 WhatsApp";
        if (Boolean.TRUE.equals(f.getCallReminder()))
            return "📞 Call";
        if (Boolean.TRUE.equals(f.getWhatsappReminder()))
            return "💬 WhatsApp";
        return "🔔";
    }
}
