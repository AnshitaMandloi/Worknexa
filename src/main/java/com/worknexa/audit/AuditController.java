package com.worknexa.audit;

import com.worknexa.audit.dto.AuditLogDto;
import com.worknexa.common.dto.ApiResponse;
import com.worknexa.common.dto.PagedResponse;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.user.User;
import com.worknexa.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'COMPLIANCE')")
@Tag(name = "Audit Trail", description = "Append-only immutable governance audit trail APIs")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get immutable audit trail filtered by requestId or tenant with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) Long requestId,
            @PageableDefault(size = 30) Pageable pageable) {

        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<AuditLog> logPage;

        if (requestId != null) {
            logPage = auditLogRepository.findAllByTenantIdAndRequestIdOrderByCreatedAtDesc(tenantId, requestId, pageable);
        } else {
            logPage = auditLogRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }

        Map<Long, String> nameCache = new HashMap<>();
        Page<AuditLogDto> dtoPage = logPage.map(log -> {
            AuditLogDto dto = AuditLogDto.fromEntity(log);
            if (log.getActorId() != null) {
                dto.setActorName(nameCache.computeIfAbsent(log.getActorId(),
                        id -> userRepository.findById(id).map(User::getName).orElse("User #" + id)));
            } else {
                dto.setActorName("SYSTEM");
            }
            return dto;
        });

        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(dtoPage)));
    }
}
