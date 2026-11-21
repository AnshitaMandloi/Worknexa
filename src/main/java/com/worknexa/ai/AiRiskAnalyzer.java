package com.worknexa.ai;

import com.worknexa.ai.dto.AiAnalysisResult;
import com.worknexa.ai.dto.RequestData;

public interface AiRiskAnalyzer {
    AiAnalysisResult analyze(RequestData data);
}
