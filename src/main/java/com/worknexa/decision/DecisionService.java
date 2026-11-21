package com.worknexa.decision;

import com.worknexa.ai.RiskLevel;
import com.worknexa.ai.dto.AiAnalysisResult;
import com.worknexa.rule.RuleAction;
import com.worknexa.rule.RuleResult;
import com.worknexa.user.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DecisionService {

    public DecisionResult makeDecision(AiAnalysisResult aiResult, RuleResult ruleResult) {
        log.info("Making decision. AI Risk: {}, Confidence: {}, Matched Rules: {}, HasMandatory: {}",
                aiResult.getRiskLevel(), aiResult.getConfidence(), ruleResult.getMatchedRules().size(), ruleResult.isHasMandatoryRule());

        // Check if any rule explicitly REJECTS
        if (ruleResult.getRequiredActions().contains(RuleAction.REJECT)) {
            return DecisionResult.builder()
                    .outcome(DecisionOutcome.REJECT)
                    .explanation("Business rule triggered automatic rejection.")
                    .mandatoryRuleTriggered(ruleResult.isHasMandatoryRule())
                    .build();
        }

        // Rule 1: Mandatory Business Rules ALWAYS override AI recommendations
        if (ruleResult.isHasMandatoryRule()) {
            List<Role> roles = determineReviewRoles(ruleResult.getMandatoryActions(), aiResult.getRiskLevel());
            return DecisionResult.builder()
                    .outcome(DecisionOutcome.HUMAN_REVIEW)
                    .requiredReviewRoles(roles)
                    .explanation("Mandatory business rule triggered human review override: " + ruleResult.getMandatoryActions())
                    .mandatoryRuleTriggered(true)
                    .build();
        }

        // Rule 2: HIGH risk -> Mandatory multi-step human review chain (Manager -> Finance/Compliance)
        if (aiResult.getRiskLevel() == RiskLevel.HIGH) {
            List<Role> roles = determineReviewRoles(ruleResult.getRequiredActions(), RiskLevel.HIGH);
            return DecisionResult.builder()
                    .outcome(DecisionOutcome.HUMAN_REVIEW)
                    .requiredReviewRoles(roles)
                    .explanation("AI assessed HIGH risk: Multi-stage executive verification required.")
                    .mandatoryRuleTriggered(false)
                    .build();
        }

        // Rule 3: MEDIUM risk OR rules require human approval -> Human review (Manager or specific roles)
        if (aiResult.getRiskLevel() == RiskLevel.MEDIUM || !ruleResult.allowsAutoApprove()) {
            List<Role> roles = determineReviewRoles(ruleResult.getRequiredActions(), aiResult.getRiskLevel());
            return DecisionResult.builder()
                    .outcome(DecisionOutcome.HUMAN_REVIEW)
                    .requiredReviewRoles(roles)
                    .explanation(aiResult.getRiskLevel() == RiskLevel.MEDIUM
                            ? "AI assessed MEDIUM risk: Standard managerial review required."
                            : "Business rules require human verification: " + ruleResult.getRequiredActions())
                    .mandatoryRuleTriggered(false)
                    .build();
        }

        // Rule 4: LOW risk AND rules allow automation -> AUTO_APPROVE
        if (aiResult.getRiskLevel() == RiskLevel.LOW && ruleResult.allowsAutoApprove()) {
            return DecisionResult.builder()
                    .outcome(DecisionOutcome.AUTO_APPROVE)
                    .requiredReviewRoles(List.of())
                    .explanation("Low risk assessed by AI and all business criteria satisfied for full automation.")
                    .mandatoryRuleTriggered(false)
                    .build();
        }

        // Safe Fallback
        return DecisionResult.builder()
                .outcome(DecisionOutcome.HUMAN_REVIEW)
                .requiredReviewRoles(List.of(Role.MANAGER))
                .explanation("Default safety policy: Routing for human review.")
                .mandatoryRuleTriggered(false)
                .build();
    }

    private List<Role> determineReviewRoles(Set<RuleAction> actions, RiskLevel riskLevel) {
        Set<Role> roles = new LinkedHashSet<>();
        // All human review flows start with MANAGER
        roles.add(Role.MANAGER);

        if (actions.contains(RuleAction.FINANCE_REVIEW) || riskLevel == RiskLevel.HIGH) {
            roles.add(Role.FINANCE);
        }

        if (actions.contains(RuleAction.COMPLIANCE_REVIEW)) {
            roles.add(Role.COMPLIANCE);
        }

        return new ArrayList<>(roles);
    }
}
