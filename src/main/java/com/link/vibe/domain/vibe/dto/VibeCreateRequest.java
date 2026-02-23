package com.link.vibe.domain.vibe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VibeCreateRequest(
        @NotBlank String sessionId,
        @NotEmpty List<Long> moodKeywordIds,
        @NotNull Long timeId,
        @NotNull Long weatherId,
        @NotNull Long placeId,
        @NotNull Long companionId
) {}
