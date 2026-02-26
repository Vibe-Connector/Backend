package com.link.vibe.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "피드 생성 요청")
public record FeedCreateRequest(
    @Schema(description = "Vibe 결과 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "result_id는 필수입니다")
    Long resultId,

    @Schema(description = "피드 캡션")
    @Size(max = 2000, message = "캡션은 2000자 이내여야 합니다")
    String caption,

    @Schema(description = "공개 여부 (기본값: false)")
    Boolean isPublic
) {}
