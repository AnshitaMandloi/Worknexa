package com.worknexa.ai;

import com.worknexa.ai.dto.AiAnalysisResult;
import com.worknexa.ai.dto.RequestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("mockRiskAnalyzer")
public class MockRiskAnalyzer implements AiRiskAnalyzer {

    @Override
    public AiAnalysisResult analyze(RequestData data) {
        log.info("MockRiskAnalyzer analyzing request: title='{}', amount={}", data.getTitle(), data.getAmount());

        BigDecimal amount = data.getAmount() != null ? data.getAmount() : BigDecimal.ZERO;
        boolean docAttached = data.isDocumentAttached();

        List<String> reasons = new ArrayList<>();
        RiskLevel level;
        int confidence;
        String recommendation;

        if (!docAttached) {
            level = RiskLevel.HIGH;
            confidence = 90;
            recommendation = "High risk: Documentation missing. Mandatory manual and compliance verification required.";
            reasons.add("Supporting documentation/invoice is missing.");
            reasons.add("High fraud and non-compliance risk without proof of expenditure.");
        } else if (amount.compareTo(new BigDecimal("10000")) <= 0) {
            level = RiskLevel.LOW;
            confidence = 94;
            recommendation = "Low financial exposure with valid documentation. Recommended for automated approval.";
            reasons.add("Amount (" + amount + ") is within low-value operational limits.");
            reasons.add("Valid supporting documentation attached.");
            reasons.add("Category '" + (data.getCategory() != null ? data.getCategory() : "General") + "' aligns with routine business expenses.");
        } else if (amount.compareTo(new BigDecimal("50000")) <= 0) {
            level = RiskLevel.MEDIUM;
            confidence = 88;
            recommendation = "Medium risk: Moderate expenditure. Recommend standard managerial review.";
            reasons.add("Amount (" + amount + ") exceeds auto-approval ceiling.");
            reasons.add("Managerial discretion recommended for budget monitoring.");
        } else {
            level = RiskLevel.HIGH;
            confidence = 95;
            recommendation = "High risk: Large financial transaction. Multi-stage human review (Manager + Finance) required.";
            reasons.add("High monetary value (" + amount + ") exceeds corporate governance threshold.");
            reasons.add("Requires multi-level executive validation and budget auditing.");
        }

        return AiAnalysisResult.builder()
                .riskLevel(level)
                .confidence(confidence)
                .recommendation(recommendation)
                .reasons(reasons)
                .build();
    }
}
