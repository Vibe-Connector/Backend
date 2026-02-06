package com.link.vibe.domain.vibe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_PROMPT = """
            당신은 감성적인 분위기 큐레이터입니다.
            사용자가 선택한 기분, 시간, 날씨, 공간, 동반자 정보를 기반으로:
            1. 해당 상황을 시적으로 요약한 한 문장의 분위기 문구 (phrase)
            2. 왜 이런 분위기가 형성되는지에 대한 감성적 분석 (analysis)

            반드시 아래 JSON 형식으로만 응답하세요:
            {
              "phrase": "시적인 한 문장 분위기 요약",
              "analysis": "이 분위기가 형성되는 이유와 감성적 맥락 설명 (2-3문장)"
            }
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.max-tokens:500}")
    private int maxTokens;

    @Value("${openai.temperature:0.8}")
    private double temperature;

    public OpenAiService(RestTemplate openAiRestTemplate, ObjectMapper objectMapper) {
        this.restTemplate = openAiRestTemplate;
        this.objectMapper = objectMapper;
    }

    public record VibeResult(String phrase, String analysis) {}

    public VibeResult generateVibe(List<String> moods, String time, String weather, String place, String companion) {
        String userPrompt = buildUserPrompt(moods, time, weather, place, companion);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", temperature,
                    "max_tokens", maxTokens,
                    "response_format", Map.of("type", "json_object")
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

            return parseResponse(response.getBody());
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패", e);
            return new VibeResult(
                    String.join(", ", moods) + " " + time + "의 분위기",
                    "AI 응답을 생성하지 못했습니다. 기본 분위기를 제공합니다."
            );
        }
    }

    public String buildUserPrompt(List<String> moods, String time, String weather, String place, String companion) {
        return String.format("""
                기분: %s
                시간: %s
                날씨: %s
                공간: %s
                동반자: %s
                """,
                String.join(", ", moods), time, weather, place, companion);
    }

    private VibeResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode result = objectMapper.readTree(content);
            return new VibeResult(
                    result.path("phrase").asText(),
                    result.path("analysis").asText()
            );
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패", e);
            return new VibeResult("분위기를 생성할 수 없습니다.", "응답 파싱에 실패했습니다.");
        }
    }
}
