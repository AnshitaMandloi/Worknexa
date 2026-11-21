package com.worknexa.approval.dto;

import com.worknexa.user.Role;
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
public class PendingTaskItemDto {
    private Long taskId;
    private Long requestId;
    private String requestTitle;
    private BigDecimal amount;
    private String category;
    private String requesterName;
    private boolean documentAttached;
    private String documentName;
    private Role assignedRole;
    private Integer sequenceNo;
    private Instant createdAt;
}
