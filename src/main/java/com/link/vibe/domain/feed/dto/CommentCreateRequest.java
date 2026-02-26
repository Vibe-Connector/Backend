package com.link.vibe.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 생성 요청")
public record CommentCreateRequest(
    @Schema(description = "댓글 내용", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 1000, message = "댓글은 1000자 이내여야 합니다")
    String content,

    @Schema(description = "부모 댓글 ID (대댓글 시). 대댓글에 답글을 달면 백엔드가 자동으로 루트 부모로 플래트닝")
    Long parentCommentId
) {}
