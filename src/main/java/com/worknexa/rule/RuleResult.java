package com.worknexa.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {

    @Builder.Default
    private List<BusinessRule> matchedRules = new ArrayList<>();

    @Builder.Default
    private Set<RuleAction> requiredActions = new HashSet<>();

    @Builder.Default
    private Set<RuleAction> mandatoryActions = new HashSet<>();

    @Builder.Default
    private boolean hasMandatoryRule = false;

    public boolean allowsAutoApprove() {
        if (hasMandatoryRule) {
            return false;
        }
        // If any human review or reject action is required, auto approve is not allowed
        return !requiredActions.contains(RuleAction.MANAGER_REVIEW)
                && !requiredActions.contains(RuleAction.FINANCE_REVIEW)
                && !requiredActions.contains(RuleAction.COMPLIANCE_REVIEW)
                && !requiredActions.contains(RuleAction.MANUAL_REVIEW)
                && !requiredActions.contains(RuleAction.REJECT);
    }
}
