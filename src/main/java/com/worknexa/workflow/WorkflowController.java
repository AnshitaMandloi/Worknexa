package com.worknexa.workflow;

import com.worknexa.common.dto.ApiResponse;
import com.worknexa.workflow.dto.CreateStageRequestDto;
import com.worknexa.workflow.dto.CreateWorkflowRequestDto;
import com.worknexa.workflow.dto.WorkflowDto;
import com.worknexa.workflow.dto.WorkflowStageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow Configuration", description = "Workflow definition and stage configuration APIs")
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    @Operation(summary = "List all workflows for organization")
    public ResponseEntity<ApiResponse<List<WorkflowDto>>> getWorkflows() {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getWorkflows()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new workflow definition (Admin only)")
    public ResponseEntity<ApiResponse<WorkflowDto>> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequestDto request) {
        WorkflowDto created = workflowService.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Workflow created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow details and stages by ID")
    public ResponseEntity<ApiResponse<WorkflowDto>> getWorkflowById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getWorkflowById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update workflow definition (Admin only)")
    public ResponseEntity<ApiResponse<WorkflowDto>> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody CreateWorkflowRequestDto request) {
        WorkflowDto updated = workflowService.updateWorkflow(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Workflow updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete workflow definition (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.ok(ApiResponse.ok("Workflow deleted successfully", null));
    }

    // --- Stages ---

    @GetMapping("/{id}/stages")
    @Operation(summary = "Get stages for a workflow")
    public ResponseEntity<ApiResponse<List<WorkflowStageDto>>> getStages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getStages(id)));
    }

    @PostMapping("/{id}/stages")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a stage to workflow (Admin only)")
    public ResponseEntity<ApiResponse<WorkflowStageDto>> addStage(
            @PathVariable Long id,
            @Valid @RequestBody CreateStageRequestDto request) {
        WorkflowStageDto stage = workflowService.addStage(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Stage added successfully", stage));
    }

    @DeleteMapping("/{id}/stages/{stageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a stage from workflow (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteStage(
            @PathVariable Long id,
            @PathVariable Long stageId) {
        workflowService.deleteStage(id, stageId);
        return ResponseEntity.ok(ApiResponse.ok("Stage deleted successfully", null));
    }
}
