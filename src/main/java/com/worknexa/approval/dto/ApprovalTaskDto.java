package com.worknexa.approval.dto;

import com.worknexa.approval.ApprovalTask;
import com.worknexa.approval.TaskStatus;
import com.worknexa.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskDto {
    private Long id;
    private Long tenantId;
    private Long requestId;
    private Role assignedRole;
    private TaskStatus status;
    private Long decidedBy;
    private String decidedByName;
    private String comment;
    private Integer sequenceNo;
    private Instant createdAt;
    private Instant decidedAt;

    public static ApprovalTaskDto fromEntity(ApprovalTask task) {
        return ApprovalTaskDto.builder()
                .id(task.getId())
                .tenantId(task.getTenantId())
                .requestId(task.getRequestId())
                .assignedRole(task.getAssignedRole())
                .status(task.getStatus())
                .decidedBy(task.getDecidedBy())
                .comment(task.getComment())
                .sequenceNo(task.getSequenceNo())
                .createdAt(task.getCreatedAt())
                .decidedAt(task.getDecidedAt())
                .build();
    }
}
