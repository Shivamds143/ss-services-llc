package com.financialcrm.repository;

import com.financialcrm.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    boolean existsByCustomerIdAndEmailTypeAndSentDate(
            Long customerId, EmailLog.EmailType emailType, LocalDate sentDate);
}