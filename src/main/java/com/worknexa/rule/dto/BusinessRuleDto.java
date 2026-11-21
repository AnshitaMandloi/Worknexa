package com.worknexa.rule.dto;

import com.worknexa.rule.BusinessRule;
import com.worknexa.rule.RuleAction;
import com.worknexa.rule.RuleOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleDto {
    private Long id;
    private Long tenantId;
    private Long workflowId;
    private String name;
    private String field;
    private RuleOperator operator;
    private String value;
    private String valueMax;
    private RuleAction action;
    private Integer priority;
    private boolean mandatory;
    private boolean active;
    private Instant createdAt;

    public static BusinessRuleDto fromEntity(BusinessRule rule) {
        return BusinessRuleDto.builder()
                .id(rule.getId())
                .tenantId(rule.getTenantId())
                .workflowId(rule.getWorkflowId())
                .name(rule.getName())
                .field(rule.getField())
                .operator(rule.getOperator())
                .value(rule.getValue())
                .valueMax(rule.getValueMax())
                .action(rule.getAction())
                .priority(rule.getPriority())
                .mandatory(rule.isMandatory())
                .active(rule.isActive())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
