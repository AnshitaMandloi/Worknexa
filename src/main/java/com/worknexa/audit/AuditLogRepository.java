package com.worknexa.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByTenantIdAndRequestIdOrderByCreatedAtAsc(Long tenantId, Long requestId);

    Page<AuditLog> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<AuditLog> findAllByTenantIdAndRequestIdOrderByCreatedAtDesc(Long tenantId, Long requestId, Pageable pageable);
}
