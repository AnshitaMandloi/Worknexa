package com.worknexa.ai.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknexa.ai.AiAnalysis;
import com.worknexa.ai.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisDto {
    private Long id;
    private Long requestId;
    private RiskLevel riskLevel;
    private Integer confidence;
    private String recommendation;
    @Builder.Default
    private List<String> reasons = new ArrayList<>();
    private Instant createdAt;

    public static AiAnalysisDto fromEntity(AiAnalysis analysis, ObjectMapper objectMapper) {
        if (analysis == null) {
            return null;
        }

        List<String> reasonsList = new ArrayList<>();
        if (analysis.getReasons() != null) {
            try {
                reasonsList = objectMapper.readValue(analysis.getReasons(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                reasonsList = List.of(analysis.getReasons());
            }
        }

        return AiAnalysisDto.builder()
                .id(analysis.getId())
                .requestId(analysis.getRequestId())
                .riskLevel(analysis.getRiskLevel())
                .confidence(analysis.getConfidence())
                .recommendation(analysis.getRecommendation())
                .reasons(reasonsList)
                .createdAt(analysis.getCreatedAt())
                .build();
    }
}
