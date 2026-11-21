package com.worknexa.workflow.dto;

import com.worknexa.user.Role;
import com.worknexa.workflow.StageType;
import com.worknexa.workflow.WorkflowStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStageDto {
    private Long id;
    private Long workflowId;
    private String name;
    private StageType stageType;
    private Role assignedRole;
    private Integer orderIndex;

    public static WorkflowStageDto fromEntity(WorkflowStage stage) {
        return WorkflowStageDto.builder()
                .id(stage.getId())
                .workflowId(stage.getWorkflowId())
                .name(stage.getName())
                .stageType(stage.getStageType())
                .assignedRole(stage.getAssignedRole())
                .orderIndex(stage.getOrderIndex())
                .build();
    }
}
