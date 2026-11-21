package com.worknexa.workflow;

import com.worknexa.audit.AuditEventType;
import com.worknexa.audit.AuditService;
import com.worknexa.common.exception.BadRequestException;
import com.worknexa.common.exception.ResourceNotFoundException;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.workflow.dto.CreateStageRequestDto;
import com.worknexa.workflow.dto.CreateWorkflowRequestDto;
import com.worknexa.workflow.dto.WorkflowDto;
import com.worknexa.workflow.dto.WorkflowStageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowRepository;
    private final WorkflowStageRepository stageRepository;
    private final AuditService auditService;

    @Transactional
    public WorkflowDto createWorkflow(CreateWorkflowRequestDto request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        if (workflowRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.getName())) {
            throw new BadRequestException("Workflow with name '" + request.getName() + "' already exists");
        }

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .requestType(request.getRequestType().trim().toUpperCase())
                .description(request.getDescription())
                .active(true)
                .build();

        workflow = workflowRepository.save(workflow);

        auditService.log(tenantId, null, AuditEventType.WORKFLOW_UPDATED, userId,
                "Created workflow definition: " + workflow.getName() + " (" + workflow.getRequestType() + ")");

        return getWorkflowById(workflow.getId());
    }

    @Transactional(readOnly = true)
    public List<WorkflowDto> getWorkflows() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        List<WorkflowDefinition> workflows = workflowRepository.findAllByTenantId(tenantId);
        return workflows.stream().map(w -> {
            WorkflowDto dto = WorkflowDto.fromEntity(w);
            List<WorkflowStage> stages = stageRepository.findAllByWorkflowIdOrderByOrderIndexAsc(w.getId());
            dto.setStages(stages.stream().map(WorkflowStageDto::fromEntity).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowDto getWorkflowById(Long id) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        WorkflowDefinition workflow = workflowRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", "id", id));

        WorkflowDto dto = WorkflowDto.fromEntity(workflow);
        List<WorkflowStage> stages = stageRepository.findAllByWorkflowIdOrderByOrderIndexAsc(id);
        dto.setStages(stages.stream().map(WorkflowStageDto::fromEntity).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public WorkflowDto updateWorkflow(Long id, CreateWorkflowRequestDto request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        WorkflowDefinition workflow = workflowRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", "id", id));

        workflow.setName(request.getName().trim());
        workflow.setRequestType(request.getRequestType().trim().toUpperCase());
        workflow.setDescription(request.getDescription());
        workflowRepository.save(workflow);

        auditService.log(tenantId, null, AuditEventType.WORKFLOW_UPDATED, userId,
                "Updated workflow: " + workflow.getName());

        return getWorkflowById(id);
    }

    @Transactional
    public void deleteWorkflow(Long id) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        WorkflowDefinition workflow = workflowRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", "id", id));

        stageRepository.deleteAllByWorkflowId(id);
        workflowRepository.delete(workflow);

        auditService.log(tenantId, null, AuditEventType.WORKFLOW_UPDATED, userId,
                "Deleted workflow ID " + id);
    }

    // --- Stages ---

    @Transactional(readOnly = true)
    public List<WorkflowStageDto> getStages(Long workflowId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        workflowRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", "id", workflowId));

        return stageRepository.findAllByWorkflowIdOrderByOrderIndexAsc(workflowId).stream()
                .map(WorkflowStageDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowStageDto addStage(Long workflowId, CreateStageRequestDto request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        WorkflowDefinition workflow = workflowRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", "id", workflowId));

        WorkflowStage stage = WorkflowStage.builder()
                .workflowId(workflow.getId())
                .name(request.getName().trim())
                .stageType(request.getStageType())
                .assignedRole(request.getAssignedRole())
                .orderIndex(request.getOrderIndex())
                .build();

        stage = stageRepository.save(stage);

        auditService.log(tenantId, null, AuditEventType.WORKFLOW_UPDATED, userId,
                "Added stage '" + stage.getName() + "' to workflow '" + workflow.getName() + "'");

        return WorkflowStageDto.fromEntity(stage);
    }

    @Transactional
    public void deleteStage(Long workflowId, Long stageId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        workflowRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", "id", workflowId));

        stageRepository.deleteById(stageId);
    }
}
