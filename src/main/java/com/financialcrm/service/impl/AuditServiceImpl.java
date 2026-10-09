package com.financialcrm.service.impl;

import com.financialcrm.entity.AuditLog;
import com.financialcrm.repository.AuditLogRepository;
import com.financialcrm.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void log(String username, String action, String entityType, Long entityId,
                    String oldValue, String newValue, String ipAddress) {
        log(username, action, entityType, entityId, oldValue, newValue, ipAddress, AuditLog.Status.SUCCESS);
    }

    @Override
    public void log(String username, String action, String entityType, Long entityId,
                    String oldValue, String newValue, String ipAddress, AuditLog.Status status) {
        AuditLog auditLog = AuditLog.builder()
            .username(username)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .oldValue(oldValue)
            .newValue(newValue)
            .ipAddress(ipAddress)
            .status(status)
            .build();
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findByOrderByCreatedAtDesc(pageable);
    }
}
