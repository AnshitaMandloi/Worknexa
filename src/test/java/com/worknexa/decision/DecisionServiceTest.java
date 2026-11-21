package com.worknexa.decision;

import com.worknexa.ai.RiskLevel;
import com.worknexa.ai.dto.AiAnalysisResult;
import com.worknexa.rule.BusinessRule;
import com.worknexa.rule.RuleAction;
import com.worknexa.rule.RuleOperator;
import com.worknexa.rule.RuleResult;
import com.worknexa.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DecisionServiceTest {

    private DecisionService decisionService;

    @BeforeEach
    void setUp() {
        decisionService = new DecisionService();
    }

    @Test
    @DisplayName("Case 1: LOW risk + rules allow automation -> AUTO_APPROVE")
    void testLowRiskAutoApproval() {
        AiAnalysisResult aiResult = AiAnalysisResult.builder()
                .riskLevel(RiskLevel.LOW)
                .confidence(95)
                .recommendation("Safe for automation")
                .reasons(List.of("Low amount", "Invoice attached"))
                .build();

        RuleResult ruleResult = RuleResult.builder()
                .requiredActions(Set.of(RuleAction.AUTO_APPROVE))
                .hasMandatoryRule(false)
                .build();

        DecisionResult decision = decisionService.makeDecision(aiResult, ruleResult);

        assertEquals(DecisionOutcome.AUTO_APPROVE, decision.getOutcome());
        assertTrue(decision.getRequiredReviewRoles().isEmpty());
        assertFalse(decision.isMandatoryRuleTriggered());
    }

    @Test
    @DisplayName("Case 2: MEDIUM risk -> HUMAN_REVIEW assigned to MANAGER")
    void testMediumRiskManagerReview() {
        AiAnalysisResult aiResult = AiAnalysisResult.builder()
                .riskLevel(RiskLevel.MEDIUM)
                .confidence(88)
                .recommendation("Manager review suggested")
                .reasons(List.of("Moderate expenditure"))
                .build();

        RuleResult ruleResult = RuleResult.builder()
                .requiredActions(Set.of(RuleAction.MANAGER_REVIEW))
                .hasMandatoryRule(false)
                .build();

        DecisionResult decision = decisionService.makeDecision(aiResult, ruleResult);

        assertEquals(DecisionOutcome.HUMAN_REVIEW, decision.getOutcome());
        assertEquals(List.of(Role.MANAGER), decision.getRequiredReviewRoles());
        assertFalse(decision.isMandatoryRuleTriggered());
    }

    @Test
    @DisplayName("Case 3: HIGH risk -> Multi-step human review chain (MANAGER -> FINANCE)")
    void testHighRiskMultiStepChain() {
        AiAnalysisResult aiResult = AiAnalysisResult.builder()
                .riskLevel(RiskLevel.HIGH)
                .confidence(96)
                .recommendation("High capital expense")
                .reasons(List.of("Amount exceeds 50000"))
                .build();

        RuleResult ruleResult = RuleResult.builder()
                .requiredActions(Set.of(RuleAction.FINANCE_REVIEW))
                .hasMandatoryRule(true)
                .mandatoryActions(Set.of(RuleAction.FINANCE_REVIEW))
                .build();

        DecisionResult decision = decisionService.makeDecision(aiResult, ruleResult);

        assertEquals(DecisionOutcome.HUMAN_REVIEW, decision.getOutcome());
        assertTrue(decision.getRequiredReviewRoles().contains(Role.MANAGER));
        assertTrue(decision.getRequiredReviewRoles().contains(Role.FINANCE));
        assertEquals(Role.MANAGER, decision.getRequiredReviewRoles().get(0));
        assertEquals(Role.FINANCE, decision.getRequiredReviewRoles().get(1));
    }

    @Test
    @DisplayName("Mandatory Rule Override: AI says LOW, but mandatory rule requires FINANCE review")
    void testMandatoryRuleOverridesAiLow() {
        AiAnalysisResult aiResult = AiAnalysisResult.builder()
                .riskLevel(RiskLevel.LOW)
                .confidence(90)
                .recommendation("Low expense")
                .reasons(List.of("Valid invoice"))
                .build();

        // Amount > 50000 triggered mandatory FINANCE_REVIEW
        RuleResult ruleResult = RuleResult.builder()
                .requiredActions(Set.of(RuleAction.FINANCE_REVIEW))
                .mandatoryActions(Set.of(RuleAction.FINANCE_REVIEW))
                .hasMandatoryRule(true)
                .build();

        DecisionResult decision = decisionService.makeDecision(aiResult, ruleResult);

        // Mandatory rule overrides AI low risk! Must require human review!
        assertEquals(DecisionOutcome.HUMAN_REVIEW, decision.getOutcome());
        assertTrue(decision.isMandatoryRuleTriggered());
        assertTrue(decision.getRequiredReviewRoles().contains(Role.FINANCE));
    }

    @Test
    @DisplayName("AI Failure Fallback: Safe fallback to MEDIUM risk -> Human review")
    void testAiFailureFallback() {
        AiAnalysisResult fallbackAiResult = AiAnalysisResult.safeFallback("Connection timeout to LLM");

        RuleResult ruleResult = RuleResult.builder()
                .requiredActions(Set.of(RuleAction.AUTO_APPROVE))
                .hasMandatoryRule(false)
                .build();

        DecisionResult decision = decisionService.makeDecision(fallbackAiResult, ruleResult);

        // Even though rule allows auto-approve, fallback MEDIUM risk routes to human review
        assertEquals(DecisionOutcome.HUMAN_REVIEW, decision.getOutcome());
        assertTrue(decision.getRequiredReviewRoles().contains(Role.MANAGER));
    }
}
