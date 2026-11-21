package com.worknexa.request.dto;

import com.worknexa.request.RequestStatus;
import com.worknexa.request.WorkflowRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestResponseDto {
    private Long id;
    private Long tenantId;
    private Long workflowId;
    private Long requesterId;
    private String requesterName;
    private String title;
    private BigDecimal amount;
    private String category;
    private String description;
    private boolean documentAttached;
    private String documentName;
    private RequestStatus status;
    private Long currentStageId;
    private String currentStageName;
    private Instant createdAt;
    private Instant updatedAt;

    public static RequestResponseDto fromEntity(WorkflowRequest request) {
        return RequestResponseDto.builder()
                .id(request.getId())
                .tenantId(request.getTenantId())
                .workflowId(request.getWorkflowId())
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .documentAttached(request.isDocumentAttached())
                .documentName(request.getDocumentName())
                .status(request.getStatus())
                .currentStageId(request.getCurrentStageId())
                .currentStageName(request.getCurrentStageName())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
