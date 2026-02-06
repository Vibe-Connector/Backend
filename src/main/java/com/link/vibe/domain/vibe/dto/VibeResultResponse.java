package com.link.vibe.domain.vibe.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VibeResultResponse(
        Long vibeId,
        String phrase,
        String analysis,
        SelectedOptions selectedOptions,
        Integer processingTimeMs,
        LocalDateTime createdAt
) {
    public record SelectedOptions(
            List<String> moods,
            String time,
            String weather,
            String place,
            String companion
    ) {}
}
