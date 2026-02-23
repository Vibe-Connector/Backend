package com.link.vibe.domain.vibe.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VibeHistoryResponse(
        Long vibeId,
        String phrase,
        List<String> moods,
        String time,
        String weather,
        String place,
        String companion,
        LocalDateTime createdAt
) {}
