package com.worknexa.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog log(Long tenantId, Long requestId, AuditEventType eventType, Long actorId, String details) {
        log.info("[AUDIT] tenantId={}, requestId={}, event={}, actorId={}, details={}",
                tenantId, requestId, eventType, actorId, details);

        AuditLog auditLog = AuditLog.builder()
                .tenantId(tenantId)
                .requestId(requestId)
                .eventType(eventType)
                .actorId(actorId)
                .details(details)
                .build();

        return auditLogRepository.save(auditLog);
    }
}
