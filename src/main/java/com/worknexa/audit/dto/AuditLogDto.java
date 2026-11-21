package com.worknexa.audit.dto;

import com.worknexa.audit.AuditEventType;
import com.worknexa.audit.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private Long tenantId;
    private Long requestId;
    private AuditEventType eventType;
    private Long actorId;
    private String actorName;
    private String details;
    private Instant createdAt;

    public static AuditLogDto fromEntity(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .requestId(log.getRequestId())
                .eventType(log.getEventType())
                .actorId(log.getActorId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
