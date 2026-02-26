package com.link.vibe.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "피드 응답")
public record FeedResponse(
    @Schema(description = "피드 ID") Long feedId,
    @Schema(description = "작성자 ID") Long userId,
    @Schema(description = "작성자 닉네임") String nickname,
    @Schema(description = "작성자 프로필 이미지") String profileImageUrl,
    @Schema(description = "Vibe 결과 ID") Long resultId,
    @Schema(description = "Vibe 생성 이미지 URL") String generatedImageUrl,
    @Schema(description = "Vibe 한줄 문구") String phrase,
    @Schema(description = "캡션") String caption,
    @Schema(description = "공개 여부") Boolean isPublic,
    @Schema(description = "조회수") Integer viewCount,
    @Schema(description = "반응 요약") List<ReactionSummary> reactions,
    @Schema(description = "댓글 수") Long commentCount,
    @Schema(description = "내 반응 유형 목록") List<String> myReactionTypes,
    @Schema(description = "작성일시") LocalDateTime createdAt,
    @Schema(description = "수정일시") LocalDateTime updatedAt
) {}
