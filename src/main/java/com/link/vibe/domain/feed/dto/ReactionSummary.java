package com.link.vibe.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "반응 요약")
public record ReactionSummary(
    @Schema(description = "반응 유형") String reactionType,
    @Schema(description = "반응 수") Long count
) {}
