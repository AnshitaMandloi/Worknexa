package com.worknexa.approval;

import com.worknexa.approval.dto.ApprovalTaskDto;
import com.worknexa.approval.dto.PendingTaskItemDto;
import com.worknexa.approval.dto.TaskDecisionDto;
import com.worknexa.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'COMPLIANCE')")
@Tag(name = "Approval Tasks", description = "Task queues and human decision execution APIs")
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    @Operation(summary = "Get actionable pending tasks assigned to current user's role")
    public ResponseEntity<ApiResponse<List<PendingTaskItemDto>>> getPendingTasks() {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.getPendingTasksForMyRole()));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Submit a reviewer decision (APPROVE, REJECT, REQUEST_INFO)")
    public ResponseEntity<ApiResponse<ApprovalTaskDto>> decideTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskDecisionDto decisionDto) {
        ApprovalTaskDto result = approvalService.decideTask(id, decisionDto);
        return ResponseEntity.ok(ApiResponse.ok("Decision processed successfully", result));
    }
}
