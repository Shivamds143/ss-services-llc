package com.financialcrm.repository;

import com.financialcrm.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);
    List<AuditLog> findTop20ByEntityTypeOrderByCreatedAtDesc(String entityType);
    List<AuditLog> findTop20ByEntityTypeAndEntityIdInOrderByCreatedAtDesc(String entityType, Collection<Long> entityIds);
}
