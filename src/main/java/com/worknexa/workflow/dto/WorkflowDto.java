package com.worknexa.workflow.dto;

import com.worknexa.workflow.WorkflowDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDto {
    private Long id;
    private Long tenantId;
    private String name;
    private String requestType;
    private String description;
    private boolean active;
    private Instant createdAt;
    @Builder.Default
    private List<WorkflowStageDto> stages = new ArrayList<>();

    public static WorkflowDto fromEntity(WorkflowDefinition workflow) {
        return WorkflowDto.builder()
                .id(workflow.getId())
                .tenantId(workflow.getTenantId())
                .name(workflow.getName())
                .requestType(workflow.getRequestType())
                .description(workflow.getDescription())
                .active(workflow.isActive())
                .createdAt(workflow.getCreatedAt())
                .build();
    }
}
