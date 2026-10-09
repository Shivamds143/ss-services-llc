package com.financialcrm.service;

import com.financialcrm.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    void log(String username, String action, String entityType, Long entityId,
             String oldValue, String newValue, String ipAddress);

    // FIX: added so callers (e.g. failed login attempts) can record the
    // correct AuditLog.Status instead of every entry always defaulting to
    // SUCCESS regardless of what actually happened.
    void log(String username, String action, String entityType, Long entityId,
             String oldValue, String newValue, String ipAddress, AuditLog.Status status);

    Page<AuditLog> findAll(Pageable pageable);
}
