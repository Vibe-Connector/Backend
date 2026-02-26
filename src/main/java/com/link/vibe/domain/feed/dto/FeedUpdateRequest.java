package com.link.vibe.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "피드 수정 요청 — caption과 isPublic만 수정 가능")
public record FeedUpdateRequest(
    @Schema(description = "피드 캡션 (null이면 미변경)")
    @Size(max = 2000, message = "캡션은 2000자 이내여야 합니다")
    String caption,

    @Schema(description = "공개 여부 (null이면 미변경)")
    Boolean isPublic
) {}
