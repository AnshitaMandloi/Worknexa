package com.worknexa.ai;

import com.worknexa.ai.dto.AiAnalysisResult;
import com.worknexa.ai.dto.RequestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Primary
public class AiRoutingService implements AiRiskAnalyzer {

    private final AiRiskAnalyzer llmRiskAnalyzer;
    private final AiRiskAnalyzer mockRiskAnalyzer;
    private final boolean mockEnabled;
    private final boolean hasApiKey;

    public AiRoutingService(
            @Qualifier("llmRiskAnalyzer") AiRiskAnalyzer llmRiskAnalyzer,
            @Qualifier("mockRiskAnalyzer") AiRiskAnalyzer mockRiskAnalyzer,
            @Value("${worknexa.ai.mock-enabled:false}") boolean mockEnabled,
            @Value("${worknexa.ai.gemini.api-key:}") String apiKey) {
        this.llmRiskAnalyzer = llmRiskAnalyzer;
        this.mockRiskAnalyzer = mockRiskAnalyzer;
        this.mockEnabled = mockEnabled;
        this.hasApiKey = StringUtils.hasText(apiKey);
    }

    @Override
    public AiAnalysisResult analyze(RequestData data) {
        if (mockEnabled || !hasApiKey) {
            log.info("Using MockRiskAnalyzer (mockEnabled={}, hasApiKey={})", mockEnabled, hasApiKey);
            return mockRiskAnalyzer.analyze(data);
        } else {
            log.info("Using LlmRiskAnalyzer (Gemini API)");
            return llmRiskAnalyzer.analyze(data);
        }
    }
}
