package com.link.vibe.domain.vibe.prompt;

import java.util.List;
import java.util.Map;

public final class VibePromptTemplates {

    private VibePromptTemplates() {}

    public static final String VIBE_SYSTEM_PROMPT = """
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

    public static String buildUserPrompt(List<String> moods, String time, String weather,
                                          String place, String companion) {
        return String.format("""
                기분: %s
                시간: %s
                날씨: %s
                공간: %s
                동반자: %s
                """,
                String.join(", ", moods), time, weather, place, companion);
    }

    public static String buildImagePrompt(String phrase, List<String> moods, String time,
                                           String weather, String place, String companion,
                                           Map<String, String> topItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a beautiful, atmospheric mood image that captures this vibe: \"").append(phrase).append("\". ");
        sb.append("The scene should reflect: ");
        sb.append("mood=").append(String.join(", ", moods));
        sb.append(", time=").append(time);
        sb.append(", weather=").append(weather);
        sb.append(", place=").append(place);
        sb.append(", companion=").append(companion).append(". ");

        if (topItems != null && !topItems.isEmpty()) {
            sb.append("Incorporate these elements subtly: ");
            topItems.forEach((category, item) -> sb.append(category).append("=").append(item).append(", "));
            sb.setLength(sb.length() - 2);
            sb.append(". ");
        }

        sb.append("Style: cinematic, warm tones, high quality, no text or words in the image.");

        String prompt = sb.toString();
        if (prompt.length() > 4000) {
            prompt = prompt.substring(0, 4000);
        }
        return prompt;
    }
}
