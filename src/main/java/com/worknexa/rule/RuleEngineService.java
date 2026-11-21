package com.worknexa.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final BusinessRuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public RuleResult evaluate(Long tenantId, Long workflowId, BigDecimal amount, Boolean documentAttached, String riskLevel, Integer riskScore) {
        List<BusinessRule> activeRules = ruleRepository.findAllByTenantIdAndWorkflowIdAndActiveTrueOrderByPriorityAsc(tenantId, workflowId);
        return evaluateRules(activeRules, amount, documentAttached, riskLevel, riskScore);
    }

    public RuleResult evaluateRules(List<BusinessRule> rules, BigDecimal amount, Boolean documentAttached, String riskLevel, Integer riskScore) {
        List<BusinessRule> matchedRules = new ArrayList<>();
        Set<RuleAction> requiredActions = new LinkedHashSet<>();
        Set<RuleAction> mandatoryActions = new LinkedHashSet<>();
        boolean hasMandatory = false;

        for (BusinessRule rule : rules) {
            if (matches(rule, amount, documentAttached, riskLevel, riskScore)) {
                matchedRules.add(rule);
                requiredActions.add(rule.getAction());
                if (rule.isMandatory()) {
                    hasMandatory = true;
                    mandatoryActions.add(rule.getAction());
                }
            }
        }

        return RuleResult.builder()
                .matchedRules(matchedRules)
                .requiredActions(requiredActions)
                .mandatoryActions(mandatoryActions)
                .hasMandatoryRule(hasMandatory)
                .build();
    }

    private boolean matches(BusinessRule rule, BigDecimal amount, Boolean documentAttached, String riskLevel, Integer riskScore) {
        String field = rule.getField().trim().toLowerCase();

        switch (field) {
            case "amount":
                return matchesAmount(rule, amount);
            case "documentattached":
            case "document_attached":
                return matchesDocumentAttached(rule, documentAttached);
            case "riskscore":
            case "risk_score":
                return matchesRiskScore(rule, riskScore);
            case "risklevel":
            case "risk_level":
            case "airisk":
            case "ai_risk":
                return matchesRiskLevel(rule, riskLevel);
            default:
                log.warn("Unknown rule field: {}", rule.getField());
                return false;
        }
    }

    private boolean matchesAmount(BusinessRule rule, BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        try {
            BigDecimal ruleVal = new BigDecimal(rule.getValue().trim());
            RuleOperator op = rule.getOperator();

            switch (op) {
                case LTE:
                    return amount.compareTo(ruleVal) <= 0;
                case LT:
                    return amount.compareTo(ruleVal) < 0;
                case GTE:
                    return amount.compareTo(ruleVal) >= 0;
                case GT:
                    return amount.compareTo(ruleVal) > 0;
                case EQ:
                    return amount.compareTo(ruleVal) == 0;
                case NEQ:
                    return amount.compareTo(ruleVal) != 0;
                case BETWEEN:
                    if (rule.getValueMax() == null) {
                        return false;
                    }
                    BigDecimal maxVal = new BigDecimal(rule.getValueMax().trim());
                    // value < amount <= valueMax (matches the prompt: 10000 < amount <= 50000)
                    return amount.compareTo(ruleVal) > 0 && amount.compareTo(maxVal) <= 0;
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("Failed to evaluate amount rule: {}", rule.getName(), e);
            return false;
        }
    }

    private boolean matchesDocumentAttached(BusinessRule rule, Boolean documentAttached) {
        boolean actual = documentAttached != null && documentAttached;
        boolean target = Boolean.parseBoolean(rule.getValue().trim());

        if (rule.getOperator() == RuleOperator.EQ) {
            return actual == target;
        } else if (rule.getOperator() == RuleOperator.NEQ) {
            return actual != target;
        }
        return false;
    }

    private boolean matchesRiskScore(BusinessRule rule, Integer riskScore) {
        if (riskScore == null) {
            return false;
        }
        try {
            int target = Integer.parseInt(rule.getValue().trim());
            RuleOperator op = rule.getOperator();
            switch (op) {
                case LTE:
                    return riskScore <= target;
                case LT:
                    return riskScore < target;
                case GTE:
                    return riskScore >= target;
                case GT:
                    return riskScore > target;
                case EQ:
                    return riskScore == target;
                case NEQ:
                    return riskScore != target;
                case BETWEEN:
                    if (rule.getValueMax() == null) return false;
                    int max = Integer.parseInt(rule.getValueMax().trim());
                    return riskScore >= target && riskScore <= max;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesRiskLevel(BusinessRule rule, String riskLevel) {
        if (riskLevel == null) {
            return false;
        }
        String target = rule.getValue().trim().toUpperCase();
        String actual = riskLevel.trim().toUpperCase();

        if (rule.getOperator() == RuleOperator.EQ) {
            return actual.equals(target);
        } else if (rule.getOperator() == RuleOperator.NEQ) {
            return !actual.equals(target);
        }
        return false;
    }
}
