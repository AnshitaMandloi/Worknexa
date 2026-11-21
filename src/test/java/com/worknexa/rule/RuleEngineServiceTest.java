package com.worknexa.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private BusinessRuleRepository ruleRepository;

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        ruleEngineService = new RuleEngineService(ruleRepository);
    }

    @Test
    @DisplayName("Should match low-value rule when amount <= 10000")
    void testLowValueRule() {
        BusinessRule rule = BusinessRule.builder()
                .name("Low value auto-approval")
                .field("amount")
                .operator(RuleOperator.LTE)
                .value("10000")
                .action(RuleAction.AUTO_APPROVE)
                .priority(10)
                .mandatory(false)
                .active(true)
                .build();

        RuleResult result = ruleEngineService.evaluateRules(List.of(rule), new BigDecimal("8000"), true, "LOW", 90);

        assertTrue(result.getMatchedRules().contains(rule));
        assertTrue(result.getRequiredActions().contains(RuleAction.AUTO_APPROVE));
        assertTrue(result.allowsAutoApprove());
        assertFalse(result.isHasMandatoryRule());
    }

    @Test
    @DisplayName("Should match between rule when 10000 < amount <= 50000")
    void testMidValueRule() {
        BusinessRule rule = BusinessRule.builder()
                .name("Mid value review")
                .field("amount")
                .operator(RuleOperator.BETWEEN)
                .value("10000")
                .valueMax("50000")
                .action(RuleAction.MANAGER_REVIEW)
                .priority(20)
                .mandatory(false)
                .active(true)
                .build();

        RuleResult result = ruleEngineService.evaluateRules(List.of(rule), new BigDecimal("35000"), true, "MEDIUM", 85);

        assertTrue(result.getMatchedRules().contains(rule));
        assertTrue(result.getRequiredActions().contains(RuleAction.MANAGER_REVIEW));
        assertFalse(result.allowsAutoApprove());
    }

    @Test
    @DisplayName("Should match mandatory high-value rule when amount > 50000")
    void testHighValueMandatoryRule() {
        BusinessRule rule = BusinessRule.builder()
                .name("High value finance oversight")
                .field("amount")
                .operator(RuleOperator.GT)
                .value("50000")
                .action(RuleAction.FINANCE_REVIEW)
                .priority(30)
                .mandatory(true)
                .active(true)
                .build();

        RuleResult result = ruleEngineService.evaluateRules(List.of(rule), new BigDecimal("200000"), true, "HIGH", 95);

        assertTrue(result.getMatchedRules().contains(rule));
        assertTrue(result.isHasMandatoryRule());
        assertTrue(result.getMandatoryActions().contains(RuleAction.FINANCE_REVIEW));
        assertFalse(result.allowsAutoApprove());
    }

    @Test
    @DisplayName("Should match mandatory document missing rule when documentAttached is false")
    void testMissingDocumentRule() {
        BusinessRule rule = BusinessRule.builder()
                .name("Missing doc audit")
                .field("documentAttached")
                .operator(RuleOperator.EQ)
                .value("false")
                .action(RuleAction.MANUAL_REVIEW)
                .priority(5)
                .mandatory(true)
                .active(true)
                .build();

        RuleResult result = ruleEngineService.evaluateRules(List.of(rule), new BigDecimal("5000"), false, "LOW", 90);

        assertTrue(result.getMatchedRules().contains(rule));
        assertTrue(result.isHasMandatoryRule());
        assertTrue(result.getMandatoryActions().contains(RuleAction.MANUAL_REVIEW));
        assertFalse(result.allowsAutoApprove());
    }

    @Test
    @DisplayName("Should match compliance review rule on HIGH risk level")
    void testHighRiskLevelRule() {
        BusinessRule rule = BusinessRule.builder()
                .name("High risk review")
                .field("riskLevel")
                .operator(RuleOperator.EQ)
                .value("HIGH")
                .action(RuleAction.COMPLIANCE_REVIEW)
                .priority(15)
                .mandatory(true)
                .active(true)
                .build();

        RuleResult result = ruleEngineService.evaluateRules(List.of(rule), new BigDecimal("100000"), true, "HIGH", 95);

        assertTrue(result.getMatchedRules().contains(rule));
        assertTrue(result.isHasMandatoryRule());
        assertTrue(result.getMandatoryActions().contains(RuleAction.COMPLIANCE_REVIEW));
    }
}
