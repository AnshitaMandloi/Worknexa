package com.worknexa.ai.dto;

import com.worknexa.ai.RiskLevel;
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
public class AiAnalysisResult {
    private RiskLevel riskLevel;
    private int confidence; // 0-100
    private String recommendation;
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    public static AiAnalysisResult safeFallback(String message) {
        return AiAnalysisResult.builder()
                .riskLevel(RiskLevel.MEDIUM)
                .confidence(50)
                .recommendation("AI unavailable: routing for human review.")
                .reasons(List.of(message != null ? message : "AI service unavailable, defaulted to safe human review"))
                .build();
    }
}
