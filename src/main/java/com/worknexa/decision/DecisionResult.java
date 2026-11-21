package com.worknexa.decision;

import com.worknexa.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionResult {
    private DecisionOutcome outcome;
    @Builder.Default
    private List<Role> requiredReviewRoles = new ArrayList<>();
    private String explanation;
    private boolean mandatoryRuleTriggered;
}
