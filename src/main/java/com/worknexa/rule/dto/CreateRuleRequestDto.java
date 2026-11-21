package com.worknexa.rule.dto;

import com.worknexa.rule.RuleAction;
import com.worknexa.rule.RuleOperator;
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
public class CreateRuleRequestDto {

    @NotBlank(message = "Rule name is required")
    @Size(min = 2, max = 120, message = "Rule name must be between 2 and 120 characters")
    private String name;

    @NotBlank(message = "Field is required (e.g. amount, documentAttached, riskScore)")
    private String field;

    @NotNull(message = "Operator is required (LTE, LT, GT, GTE, EQ, NEQ, BETWEEN)")
    private RuleOperator operator;

    @NotBlank(message = "Value is required")
    private String value;

    private String valueMax; // for BETWEEN

    @NotNull(message = "Action is required")
    private RuleAction action;

    @NotNull(message = "Priority is required")
    private Integer priority;

    @Builder.Default
    private boolean mandatory = false;

    @Builder.Default
    private boolean active = true;
}
