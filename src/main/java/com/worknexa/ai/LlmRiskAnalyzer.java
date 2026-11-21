package com.worknexa.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknexa.ai.dto.AiAnalysisResult;
import com.worknexa.ai.dto.RequestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("llmRiskAnalyzer")
public class LlmRiskAnalyzer implements AiRiskAnalyzer {

    private final String apiKey;
    private final String model;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    public LlmRiskAnalyzer(
            @Value("${worknexa.ai.gemini.api-key:}") String apiKey,
            @Value("${worknexa.ai.gemini.model:gemini-3.5-flash-lite}") String model,
            @Value("${worknexa.ai.gemini.timeout-ms:10000}") int timeoutMs,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = StringUtils.hasText(model) ? model.trim() : "gemini-3.5-flash-lite";
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 10000;
        this.objectMapper = objectMapper;
    }

    private synchronized HttpClient getHttpClient() {
        if (this.httpClient == null) {
            try {
                this.httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(this.timeoutMs))
                        .build();
            } catch (Exception e) {
                log.warn("Unable to initialize HttpClient in this environment: {}", e.getMessage());
                return null;
            }
        }
        return this.httpClient;
    }

    @Override
    public AiAnalysisResult analyze(RequestData data) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("Gemini API key is not configured. Falling back to safe analysis result.");
            return AiAnalysisResult.safeFallback("Gemini API key is missing");
        }

        try {
            HttpClient client = getHttpClient();
            if (client == null) {
                return AiAnalysisResult.safeFallback("HTTP client unavailable in sandbox");
            }
            String prompt = buildPrompt(data);
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "responseMimeType", "application/json"
                    )
            ));

            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent",
                    model
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API call failed with status code: {} and body: {}", response.statusCode(), response.body());
                return AiAnalysisResult.safeFallback("LLM API returned HTTP " + response.statusCode()
                        + (response.statusCode() == 404 ? " (check the GEMINI_MODEL name)" : ""));
            }

            return parseAndValidateGeminiResponse(response.body());

        } catch (Exception ex) {
            log.error("Exception during LLM risk analysis: {}", ex.getMessage(), ex);
            return AiAnalysisResult.safeFallback("AI analysis error: " + ex.getMessage());
        }
    }

    private String buildPrompt(RequestData data) {
        return "You are an enterprise compliance and workflow risk assessment AI.\n" +
                "Analyze the following corporate request and respond with STRICT JSON ONLY matching this schema:\n" +
                "{\n" +
                "  \"riskLevel\": \"LOW\" | \"MEDIUM\" | \"HIGH\",\n" +
                "  \"confidence\": 0-100,\n" +
                "  \"recommendation\": \"string explanation\",\n" +
                "  \"reasons\": [\"reason 1\", \"reason 2\"]\n" +
                "}\n\n" +
                "Request details:\n" +
                "- Title: " + (data.getTitle() != null ? data.getTitle() : "N/A") + "\n" +
                "- Amount: " + (data.getAmount() != null ? data.getAmount().toPlainString() : "0") + "\n" +
                "- Category: " + (data.getCategory() != null ? data.getCategory() : "General") + "\n" +
                "- Description: " + (data.getDescription() != null ? data.getDescription() : "N/A") + "\n" +
                "- Document Attached: " + data.isDocumentAttached() + "\n";
    }

    public AiAnalysisResult parseAndValidateGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) {
                return AiAnalysisResult.safeFallback("No candidates returned from LLM");
            }

            // Newer Gemini models can return several parts; use the first real text part (skip "thought" parts)
            JsonNode parts = candidates.get(0).path("content").path("parts");
            JsonNode textNode = null;
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (!part.path("thought").asBoolean(false) && part.hasNonNull("text")) {
                        textNode = part.path("text");
                        break;
                    }
                }
            }
            if (textNode == null) {
                return AiAnalysisResult.safeFallback("Empty text part in LLM response");
            }

            String contentText = textNode.asText().trim();
            // Clean markdown code blocks if any
            if (contentText.startsWith("```json")) {
                contentText = contentText.substring(7);
            } else if (contentText.startsWith("```")) {
                contentText = contentText.substring(3);
            }
            if (contentText.endsWith("```")) {
                contentText = contentText.substring(0, contentText.length() - 3);
            }
            contentText = contentText.trim();

            JsonNode resultNode = objectMapper.readTree(contentText);

            String riskLevelStr = resultNode.path("riskLevel").asText().trim().toUpperCase();
            RiskLevel riskLevel;
            try {
                riskLevel = RiskLevel.valueOf(riskLevelStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid risk level received from AI: {}", riskLevelStr);
                return AiAnalysisResult.safeFallback("Invalid risk level returned by AI");
            }

            int confidence = resultNode.path("confidence").asInt(50);
            if (confidence < 0 || confidence > 100) {
                confidence = Math.max(0, Math.min(100, confidence));
            }

            String recommendation = resultNode.path("recommendation").asText();
            if (!StringUtils.hasText(recommendation)) {
                recommendation = "AI review completed with risk level: " + riskLevel;
            }

            List<String> reasons = new ArrayList<>();
            JsonNode reasonsNode = resultNode.path("reasons");
            if (reasonsNode.isArray()) {
                for (JsonNode r : reasonsNode) {
                    if (StringUtils.hasText(r.asText())) {
                        reasons.add(r.asText().trim());
                    }
                }
            }
            if (reasons.isEmpty()) {
                reasons.add("Risk assessed based on financial threshold and documentation verification.");
            }

            return AiAnalysisResult.builder()
                    .riskLevel(riskLevel)
                    .confidence(confidence)
                    .recommendation(recommendation)
                    .reasons(reasons)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse LLM JSON output: {}", e.getMessage());
            return AiAnalysisResult.safeFallback("Failed to parse AI output: " + e.getMessage());
        }
    }
}