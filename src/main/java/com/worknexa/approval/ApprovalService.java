package com.worknexa.approval;

import com.worknexa.approval.dto.ApprovalTaskDto;
import com.worknexa.approval.dto.PendingTaskItemDto;
import com.worknexa.approval.dto.TaskDecisionDto;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.request.WorkflowEngineService;
import com.worknexa.request.WorkflowRequest;
import com.worknexa.request.WorkflowRequestRepository;
import com.worknexa.user.Role;
import com.worknexa.user.User;
import com.worknexa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalTaskRepository taskRepository;
    private final WorkflowRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final WorkflowEngineService workflowEngineService;

    @Transactional(readOnly = true)
    public List<PendingTaskItemDto> getPendingTasksForMyRole() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Role userRole = SecurityUtils.getCurrentUserRole();

        List<ApprovalTask> tasks = taskRepository.findActionableTasksForRole(tenantId, userRole);

        return tasks.stream().map(task -> {
            Optional<WorkflowRequest> reqOpt = requestRepository.findByIdAndTenantId(task.getRequestId(), tenantId);
            String title = "N/A";
            java.math.BigDecimal amount = java.math.BigDecimal.ZERO;
            String category = "N/A";
            String requesterName = "Unknown";
            boolean docAttached = false;
            String docName = null;

            if (reqOpt.isPresent()) {
                WorkflowRequest req = reqOpt.get();
                title = req.getTitle();
                amount = req.getAmount();
                category = req.getCategory();
                docAttached = req.isDocumentAttached();
                docName = req.getDocumentName();
                requesterName = userRepository.findById(req.getRequesterId())
                        .map(User::getName).orElse("User #" + req.getRequesterId());
            }

            return PendingTaskItemDto.builder()
                    .taskId(task.getId())
                    .requestId(task.getRequestId())
                    .requestTitle(title)
                    .amount(amount)
                    .category(category)
                    .requesterName(requesterName)
                    .documentAttached(docAttached)
                    .documentName(docName)
                    .assignedRole(task.getAssignedRole())
                    .sequenceNo(task.getSequenceNo())
                    .createdAt(task.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public ApprovalTaskDto decideTask(Long taskId, TaskDecisionDto decisionDto) {
        ApprovalTask updatedTask = workflowEngineService.processTaskDecision(
                taskId, decisionDto.getDecision(), decisionDto.getComment());
        return ApprovalTaskDto.fromEntity(updatedTask);
    }
}
