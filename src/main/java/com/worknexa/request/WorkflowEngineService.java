package com.worknexa.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknexa.ai.AiAnalysis;
import com.worknexa.ai.AiAnalysisRepository;
import com.worknexa.ai.AiRiskAnalyzer;
import com.worknexa.ai.dto.AiAnalysisResult;
import com.worknexa.ai.dto.RequestData;
import com.worknexa.approval.ApprovalTask;
import com.worknexa.approval.ApprovalTaskRepository;
import com.worknexa.approval.TaskStatus;
import com.worknexa.audit.AuditEventType;
import com.worknexa.audit.AuditService;
import com.worknexa.common.exception.BadRequestException;
import com.worknexa.common.exception.ResourceNotFoundException;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.decision.DecisionOutcome;
import com.worknexa.decision.DecisionResult;
import com.worknexa.decision.DecisionService;
import com.worknexa.notification.NotificationService;
import com.worknexa.rule.BusinessRule;
import com.worknexa.rule.BusinessRuleRepository;
import com.worknexa.rule.RuleEngineService;
import com.worknexa.rule.RuleResult;
import com.worknexa.user.Role;
import com.worknexa.workflow.StageType;
import com.worknexa.workflow.WorkflowDefinition;
import com.worknexa.workflow.WorkflowDefinitionRepository;
import com.worknexa.workflow.WorkflowStage;
import com.worknexa.workflow.WorkflowStageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngineService {

    private final WorkflowRequestRepository requestRepository;
    private final WorkflowDefinitionRepository workflowRepository;
    private final WorkflowStageRepository stageRepository;
    private final BusinessRuleRepository ruleRepository;
    private final ApprovalTaskRepository taskRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final AiRiskAnalyzer aiRiskAnalyzer;
    private final RuleEngineService ruleEngineService;
    private final DecisionService decisionService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowRequest submitRequest(WorkflowRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long requesterId = SecurityUtils.getCurrentUserId();

        request.setTenantId(tenantId);
        request.setRequesterId(requesterId);
        request.setStatus(RequestStatus.SUBMITTED);

        // Find matching active workflow definition
        String requestType = request.getCategory() != null ? request.getCategory() : "EXPENSE";
        WorkflowDefinition workflow = workflowRepository
                .findByTenantIdAndRequestTypeIgnoreCaseAndActiveTrue(tenantId, requestType)
                .or(() -> workflowRepository.findAllByTenantId(tenantId).stream().filter(WorkflowDefinition::isActive).findFirst())
                .orElseThrow(() -> new BadRequestException("No active workflow definition configured for organization"));

        request.setWorkflowId(workflow.getId());

        List<WorkflowStage> stages = stageRepository.findAllByWorkflowIdOrderByOrderIndexAsc(workflow.getId());
        WorkflowStage submissionStage = stages.stream()
                .filter(s -> s.getStageType() == StageType.SUBMISSION)
                .findFirst()
                .orElse(null);

        if (submissionStage != null) {
            request.setCurrentStageId(submissionStage.getId());
            request.setCurrentStageName(submissionStage.getName());
        }

        request = requestRepository.save(request);

        auditService.log(tenantId, request.getId(), AuditEventType.REQUEST_CREATED, requesterId,
                String.format("Request '%s' created for amount %s. Category: %s, Document attached: %s",
                        request.getTitle(), request.getAmount(), request.getCategory(), request.isDocumentAttached()));

        // Advance to AI_ANALYSIS
        WorkflowStage aiStage = stages.stream()
                .filter(s -> s.getStageType() == StageType.AI_ANALYSIS)
                .findFirst()
                .orElse(null);

        if (aiStage != null) {
            request.setCurrentStageId(aiStage.getId());
            request.setCurrentStageName(aiStage.getName());
        }
        request.setStatus(RequestStatus.AI_ANALYZING);
        request = requestRepository.save(request);

        auditService.log(tenantId, request.getId(), AuditEventType.AI_ANALYSIS_STARTED, null,
                "AI risk analysis initiated.");

        // AI Risk Analysis
        RequestData requestData = RequestData.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .documentAttached(request.isDocumentAttached())
                .build();

        AiAnalysisResult aiResult = aiRiskAnalyzer.analyze(requestData);

        String reasonsJson;
        try {
            reasonsJson = objectMapper.writeValueAsString(aiResult.getReasons());
        } catch (JsonProcessingException e) {
            reasonsJson = String.join("; ", aiResult.getReasons());
        }

        AiAnalysis aiAnalysis = AiAnalysis.builder()
                .requestId(request.getId())
                .riskLevel(aiResult.getRiskLevel())
                .confidence(aiResult.getConfidence())
                .recommendation(aiResult.getRecommendation())
                .reasons(reasonsJson)
                .build();
        aiAnalysisRepository.save(aiAnalysis);

        auditService.log(tenantId, request.getId(), AuditEventType.AI_ANALYSIS_COMPLETED, null,
                String.format("AI Risk: %s, Confidence: %d%%, Recommendation: %s",
                        aiResult.getRiskLevel(), aiResult.getConfidence(), aiResult.getRecommendation()));

        // Rule Evaluation
        RuleResult ruleResult = ruleEngineService.evaluate(
                tenantId,
                workflow.getId(),
                request.getAmount(),
                request.isDocumentAttached(),
                aiResult.getRiskLevel().name(),
                aiResult.getConfidence()
        );

        for (BusinessRule matchedRule : ruleResult.getMatchedRules()) {
            auditService.log(tenantId, request.getId(), AuditEventType.RULE_TRIGGERED, null,
                    String.format("Rule '%s' matched: %s on %s %s %s (Mandatory: %s)",
                            matchedRule.getName(), matchedRule.getAction(), matchedRule.getField(),
                            matchedRule.getOperator(), matchedRule.getValue(), matchedRule.isMandatory()));
        }

        // Decision Layer
        DecisionResult decision = decisionService.makeDecision(aiResult, ruleResult);

        auditService.log(tenantId, request.getId(), AuditEventType.DECISION_MADE, null,
                String.format("Outcome: %s. Explanation: %s", decision.getOutcome(), decision.getExplanation()));

        // Apply Decision
        WorkflowStage humanReviewStage = stages.stream()
                .filter(s -> s.getStageType() == StageType.HUMAN_REVIEW)
                .findFirst()
                .orElse(null);

        WorkflowStage completionStage = stages.stream()
                .filter(s -> s.getStageType() == StageType.COMPLETION)
                .findFirst()
                .orElse(null);

        if (decision.getOutcome() == DecisionOutcome.AUTO_APPROVE) {
            request.setStatus(RequestStatus.AUTO_APPROVED);
            if (completionStage != null) {
                request.setCurrentStageId(completionStage.getId());
                request.setCurrentStageName(completionStage.getName());
            }
            request = requestRepository.save(request);

            auditService.log(tenantId, request.getId(), AuditEventType.REQUEST_AUTO_APPROVED, null,
                    "Request was automatically approved by the platform.");

            notificationService.createNotification(tenantId, requesterId,
                    String.format("Your request '%s' has been automatically approved.", request.getTitle()));

        } else if (decision.getOutcome() == DecisionOutcome.REJECT) {
            request.setStatus(RequestStatus.REJECTED);
            if (completionStage != null) {
                request.setCurrentStageId(completionStage.getId());
                request.setCurrentStageName(completionStage.getName());
            }
            request = requestRepository.save(request);

            auditService.log(tenantId, request.getId(), AuditEventType.REQUEST_REJECTED, null,
                    "Request rejected: " + decision.getExplanation());

            notificationService.createNotification(tenantId, requesterId,
                    String.format("Your request '%s' has been rejected: %s", request.getTitle(), decision.getExplanation()));

        } else {
            // HUMAN_REVIEW: Pause workflow and generate approval task(s)
            request.setStatus(RequestStatus.PENDING_REVIEW);
            if (humanReviewStage != null) {
                request.setCurrentStageId(humanReviewStage.getId());
                request.setCurrentStageName(humanReviewStage.getName());
            }
            request = requestRepository.save(request);

            List<Role> roles = decision.getRequiredReviewRoles();
            for (int i = 0; i < roles.size(); i++) {
                Role role = roles.get(i);
                int sequenceNo = i + 1;
                ApprovalTask task = ApprovalTask.builder()
                        .tenantId(tenantId)
                        .requestId(request.getId())
                        .assignedRole(role)
                        .sequenceNo(sequenceNo)
                        .status(TaskStatus.PENDING)
                        .build();
                taskRepository.save(task);

                auditService.log(tenantId, request.getId(), AuditEventType.TASK_ASSIGNED, null,
                        String.format("Approval task (Step %d) assigned to %s", sequenceNo, role));

                if (i == 0) {
                    notificationService.notifyRole(tenantId, role,
                            String.format("New approval task assigned: Request #%d '%s' (Amount: %s)",
                                    request.getId(), request.getTitle(), request.getAmount()));
                }
            }
        }

        return request;
    }

    @Transactional
    public ApprovalTask processTaskDecision(Long taskId, String decisionStr, String comment) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();
        Role userRole = SecurityUtils.getCurrentUserRole();

        ApprovalTask task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalTask", "id", taskId));

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new BadRequestException("Task has already been decided: " + task.getStatus());
        }

        // Enforce RBAC: user's role must match assignedRole (or ADMIN acting as proxy)
        if (userRole != task.getAssignedRole() && userRole != Role.ADMIN) {
            throw new AccessDeniedException(String.format("Role %s cannot decide a task assigned to %s", userRole, task.getAssignedRole()));
        }

        final Long currentRequestId = task.getRequestId();
        final int currentSequenceNo = task.getSequenceNo();

        WorkflowRequest request = requestRepository.findByIdAndTenantId(currentRequestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRequest", "id", currentRequestId));

        // Separation of duties: a requester can never decide their own request
        if (request.getRequesterId() != null && request.getRequesterId().equals(userId)) {
            throw new AccessDeniedException("You cannot decide a request that you submitted yourself");
        }

        List<WorkflowStage> stages = stageRepository.findAllByWorkflowIdOrderByOrderIndexAsc(request.getWorkflowId());
        WorkflowStage completionStage = stages.stream()
                .filter(s -> s.getStageType() == StageType.COMPLETION)
                .findFirst()
                .orElse(null);

        String decision = decisionStr.trim().toUpperCase();

        if ("REJECT".equals(decision)) {
            task.setStatus(TaskStatus.REJECTED);
            task.setDecidedBy(userId);
            task.setDecidedAt(Instant.now());
            task.setComment(comment);
            task = taskRepository.save(task);

            request.setStatus(RequestStatus.REJECTED);
            if (completionStage != null) {
                request.setCurrentStageId(completionStage.getId());
                request.setCurrentStageName(completionStage.getName());
            }
            requestRepository.save(request);

            // Close the rest of the chain so no orphan PENDING tasks remain
            taskRepository.findAllByTenantIdAndRequestIdOrderBySequenceNoAsc(tenantId, request.getId()).stream()
                    .filter(t -> t.getStatus() == TaskStatus.PENDING && !t.getId().equals(taskId))
                    .forEach(t -> {
                        t.setStatus(TaskStatus.CANCELLED);
                        taskRepository.save(t);
                    });

            auditService.log(tenantId, request.getId(), AuditEventType.TASK_REJECTED, userId,
                    String.format("Task rejected by %s: %s", userRole, comment));

            auditService.log(tenantId, request.getId(), AuditEventType.REQUEST_REJECTED, userId,
                    "Request marked REJECTED following reviewer rejection.");

            notificationService.createNotification(tenantId, request.getRequesterId(),
                    String.format("Your request '%s' was rejected by %s: %s", request.getTitle(), userRole, comment));

        } else if ("REQUEST_INFO".equals(decision)) {
            task.setStatus(TaskStatus.INFO_REQUESTED);
            task.setDecidedBy(userId);
            task.setDecidedAt(Instant.now());
            task.setComment(comment);
            task = taskRepository.save(task);

            request.setStatus(RequestStatus.INFO_REQUESTED);
            requestRepository.save(request);

            auditService.log(tenantId, request.getId(), AuditEventType.INFO_REQUESTED, userId,
                    String.format("Information requested by %s: %s", userRole, comment));

            notificationService.createNotification(tenantId, request.getRequesterId(),
                    String.format("Information requested for your submission '%s': %s", request.getTitle(), comment));

        } else if ("APPROVE".equals(decision)) {
            task.setStatus(TaskStatus.APPROVED);
            task.setDecidedBy(userId);
            task.setDecidedAt(Instant.now());
            task.setComment(comment);
            task = taskRepository.save(task);

            auditService.log(tenantId, request.getId(), AuditEventType.TASK_APPROVED, userId,
                    String.format("Step %d approved by %s: %s", task.getSequenceNo(), userRole, comment != null ? comment : "Approved"));

            // Check if there is a subsequent task in sequence
            List<ApprovalTask> allTasks = taskRepository.findAllByTenantIdAndRequestIdOrderBySequenceNoAsc(tenantId, request.getId());
            Optional<ApprovalTask> nextTaskOpt = allTasks.stream()
                    .filter(t -> t.getSequenceNo() > currentSequenceNo && t.getStatus() == TaskStatus.PENDING)
                    .findFirst();

            if (nextTaskOpt.isPresent()) {
                ApprovalTask nextTask = nextTaskOpt.get();
                auditService.log(tenantId, request.getId(), AuditEventType.TASK_ASSIGNED, null,
                        String.format("Advancing to Step %d assigned to %s", nextTask.getSequenceNo(), nextTask.getAssignedRole()));

                notificationService.notifyRole(tenantId, nextTask.getAssignedRole(),
                        String.format("New approval task assigned: Request #%d '%s' (Step %d)",
                                request.getId(), request.getTitle(), nextTask.getSequenceNo()));
            } else {
                // All tasks in the chain are approved!
                request.setStatus(RequestStatus.APPROVED);
                if (completionStage != null) {
                    request.setCurrentStageId(completionStage.getId());
                    request.setCurrentStageName(completionStage.getName());
                }
                requestRepository.save(request);

                auditService.log(tenantId, request.getId(), AuditEventType.REQUEST_APPROVED, userId,
                        "All approval steps completed. Request is fully APPROVED.");

                notificationService.createNotification(tenantId, request.getRequesterId(),
                        String.format("Your request '%s' has been fully approved!", request.getTitle()));
            }
        } else {
            throw new BadRequestException("Invalid decision: " + decisionStr + ". Allowed values: APPROVE, REJECT, REQUEST_INFO");
        }

        return task;
    }

    @Transactional
    public WorkflowRequest respondToInfoRequest(Long requestId, String comment, Boolean documentAttached, String documentName) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        WorkflowRequest request = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRequest", "id", requestId));

        if (request.getStatus() != RequestStatus.INFO_REQUESTED) {
            throw new BadRequestException("Request is not awaiting information. Current status: " + request.getStatus());
        }

        if (documentAttached != null) {
            request.setDocumentAttached(documentAttached);
        }
        if (documentName != null) {
            request.setDocumentName(documentName);
        }

        // Resume workflow: set status back to PENDING_REVIEW
        request.setStatus(RequestStatus.PENDING_REVIEW);
        request = requestRepository.save(request);

        // Find the task that requested info and reset its status to PENDING
        List<ApprovalTask> tasks = taskRepository.findAllByTenantIdAndRequestIdOrderBySequenceNoAsc(tenantId, requestId);
        ApprovalTask infoTask = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.INFO_REQUESTED)
                .findFirst()
                .orElse(null);

        if (infoTask != null) {
            infoTask.setStatus(TaskStatus.PENDING);
            taskRepository.save(infoTask);

            auditService.log(tenantId, requestId, AuditEventType.INFO_PROVIDED, userId,
                    String.format("Information provided by requester: %s (Document: %s)",
                            comment != null ? comment : "Resubmitted", request.getDocumentName()));

            notificationService.notifyRole(tenantId, infoTask.getAssignedRole(),
                    String.format("Information provided for Request #%d '%s'. Ready for review.",
                            request.getId(), request.getTitle()));
        }

        return request;
    }
}