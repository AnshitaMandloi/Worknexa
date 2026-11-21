package com.worknexa.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknexa.ai.AiAnalysis;
import com.worknexa.ai.AiAnalysisRepository;
import com.worknexa.ai.dto.AiAnalysisDto;
import com.worknexa.approval.ApprovalTask;
import com.worknexa.approval.ApprovalTaskRepository;
import com.worknexa.approval.dto.ApprovalTaskDto;
import com.worknexa.audit.AuditLog;
import com.worknexa.audit.AuditLogRepository;
import com.worknexa.audit.dto.AuditLogDto;
import com.worknexa.common.exception.ResourceNotFoundException;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.request.dto.CreateRequestDto;
import com.worknexa.request.dto.RequestDetailDto;
import com.worknexa.request.dto.RequestResponseDto;
import com.worknexa.request.dto.RespondInfoRequestDto;
import com.worknexa.user.Role;
import com.worknexa.user.User;
import com.worknexa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final WorkflowRequestRepository requestRepository;
    private final WorkflowEngineService workflowEngineService;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final ApprovalTaskRepository taskRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public RequestResponseDto submitRequest(CreateRequestDto dto) {
        WorkflowRequest request = WorkflowRequest.builder()
                .title(dto.getTitle().trim())
                .amount(dto.getAmount())
                .category(dto.getCategory().trim().toUpperCase())
                .description(dto.getDescription())
                .documentAttached(dto.isDocumentAttached())
                .documentName(dto.getDocumentName())
                .build();

        WorkflowRequest saved = workflowEngineService.submitRequest(request);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<RequestResponseDto> getMyRequests(Pageable pageable) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        return requestRepository.findAllByTenantIdAndRequesterId(tenantId, userId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<RequestResponseDto> getAllRequests(Pageable pageable) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return requestRepository.findAllByTenantId(tenantId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public RequestDetailDto getRequestDetail(Long id) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();
        Role userRole = SecurityUtils.getCurrentUserRole();

        WorkflowRequest request = requestRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRequest", "id", id));

        // If user is EMPLOYEE, ensure they own the request
        if (userRole == Role.EMPLOYEE && !request.getRequesterId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to view this request");
        }

        AiAnalysis aiAnalysis = aiAnalysisRepository.findByRequestId(id).orElse(null);
        AiAnalysisDto aiDto = AiAnalysisDto.fromEntity(aiAnalysis, objectMapper);

        List<ApprovalTask> tasks = taskRepository.findAllByTenantIdAndRequestIdOrderBySequenceNoAsc(tenantId, id);
        List<AuditLog> logs = auditLogRepository.findAllByTenantIdAndRequestIdOrderByCreatedAtAsc(tenantId, id);

        // Map user names for reviewers and audit actors
        Map<Long, String> userNameCache = new HashMap<>();

        List<ApprovalTaskDto> taskDtos = tasks.stream().map(t -> {
            ApprovalTaskDto dto = ApprovalTaskDto.fromEntity(t);
            if (t.getDecidedBy() != null) {
                dto.setDecidedByName(getUserName(t.getDecidedBy(), userNameCache));
            }
            return dto;
        }).collect(Collectors.toList());

        List<AuditLogDto> logDtos = logs.stream().map(l -> {
            AuditLogDto dto = AuditLogDto.fromEntity(l);
            if (l.getActorId() != null) {
                dto.setActorName(getUserName(l.getActorId(), userNameCache));
            } else {
                dto.setActorName("SYSTEM");
            }
            return dto;
        }).collect(Collectors.toList());

        RequestResponseDto requestDto = mapToDto(request);

        return RequestDetailDto.builder()
                .request(requestDto)
                .aiAnalysis(aiDto)
                .tasks(taskDtos)
                .timeline(logDtos)
                .build();
    }

    @Transactional
    public RequestResponseDto respondToInfo(Long id, RespondInfoRequestDto dto) {
        WorkflowRequest updated = workflowEngineService.respondToInfoRequest(
                id, dto.getComment(), dto.getDocumentAttached(), dto.getDocumentName());
        return mapToDto(updated);
    }

    private RequestResponseDto mapToDto(WorkflowRequest request) {
        RequestResponseDto dto = RequestResponseDto.fromEntity(request);
        userRepository.findById(request.getRequesterId())
                .ifPresent(u -> dto.setRequesterName(u.getName()));
        return dto;
    }

    private String getUserName(Long userId, Map<Long, String> cache) {
        return cache.computeIfAbsent(userId, id ->
                userRepository.findById(id).map(User::getName).orElse("User #" + id));
    }
}
