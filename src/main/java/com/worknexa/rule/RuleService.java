package com.worknexa.rule;

import com.worknexa.audit.AuditEventType;
import com.worknexa.audit.AuditService;
import com.worknexa.common.exception.ResourceNotFoundException;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.rule.dto.BusinessRuleDto;
import com.worknexa.rule.dto.CreateRuleRequestDto;
import com.worknexa.workflow.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService {

    private final BusinessRuleRepository ruleRepository;
    private final WorkflowDefinitionRepository workflowRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BusinessRuleDto> getRulesByWorkflow(Long workflowId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        workflowRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", "id", workflowId));

        return ruleRepository.findAllByTenantIdAndWorkflowIdOrderByPriorityAsc(tenantId, workflowId)
                .stream()
                .map(BusinessRuleDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public BusinessRuleDto createRule(Long workflowId, CreateRuleRequestDto request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        workflowRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", "id", workflowId));

        BusinessRule rule = BusinessRule.builder()
                .tenantId(tenantId)
                .workflowId(workflowId)
                .name(request.getName().trim())
                .field(request.getField().trim())
                .operator(request.getOperator())
                .value(request.getValue().trim())
                .valueMax(request.getValueMax() != null ? request.getValueMax().trim() : null)
                .action(request.getAction())
                .priority(request.getPriority())
                .mandatory(request.isMandatory())
                .active(request.isActive())
                .build();

        rule = ruleRepository.save(rule);

        auditService.log(tenantId, null, AuditEventType.RULE_UPDATED, userId,
                "Created business rule '" + rule.getName() + "' for workflow ID " + workflowId);

        return BusinessRuleDto.fromEntity(rule);
    }

    @Transactional
    public BusinessRuleDto updateRule(Long workflowId, Long ruleId, CreateRuleRequestDto request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        BusinessRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessRule", "id", ruleId));

        rule.setName(request.getName().trim());
        rule.setField(request.getField().trim());
        rule.setOperator(request.getOperator());
        rule.setValue(request.getValue().trim());
        rule.setValueMax(request.getValueMax() != null ? request.getValueMax().trim() : null);
        rule.setAction(request.getAction());
        rule.setPriority(request.getPriority());
        rule.setMandatory(request.isMandatory());
        rule.setActive(request.isActive());

        rule = ruleRepository.save(rule);

        auditService.log(tenantId, null, AuditEventType.RULE_UPDATED, userId,
                "Updated business rule '" + rule.getName() + "' (ID " + ruleId + ")");

        return BusinessRuleDto.fromEntity(rule);
    }

    @Transactional
    public void deleteRule(Long workflowId, Long ruleId) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        BusinessRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessRule", "id", ruleId));

        ruleRepository.delete(rule);

        auditService.log(tenantId, null, AuditEventType.RULE_UPDATED, userId,
                "Deleted business rule ID " + ruleId);
    }
}
