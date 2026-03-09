package com.link.vibe.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "비슷한 무드 피드 응답")
public record SimilarFeedResponse(
    @Schema(description = "피드 ID") Long feedId,
    @Schema(description = "Vibe 결과 ID") Long resultId,
    @Schema(description = "Vibe 생성 이미지 URL") String generatedImageUrl,
    @Schema(description = "Vibe 한줄 문구") String phrase,
    @Schema(description = "캡션") String caption,
    @Schema(description = "작성자 ID") Long authorId,
    @Schema(description = "작성자 닉네임") String authorNickname,
    @Schema(description = "작성자 프로필 이미지") String authorProfileImageUrl,
    @Schema(description = "유사도 점수") Integer similarityScore,
    @Schema(description = "작성일시") LocalDateTime createdAt
) {}
