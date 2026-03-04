package com.link.vibe.domain.explore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "인기 Vibe 탐색 응답")
public record ExploreVibeResponse(
    @Schema(description = "피드 ID", example = "42")
    Long feedId,

    @Schema(description = "Vibe 결과 ID", example = "15")
    Long resultId,

    @Schema(description = "Vibe 결과 이미지 URL", example = "https://cdn.example.com/vibes/img_42.png")
    String generatedImageUrl,

    @Schema(description = "피드 캡션", example = "오늘의 무드")
    String caption,

    @Schema(description = "작성자 ID", example = "7")
    Long authorId,

    @Schema(description = "작성자 닉네임", example = "지연")
    String authorNickname,

    @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.example.com/profiles/7.jpg")
    String authorProfileImageUrl,

    @Schema(description = "조회수", example = "50")
    Integer viewCount,

    @Schema(description = "반응 수", example = "12")
    Long reactionCount,

    @Schema(description = "댓글 수 (본인 댓글 제외)", example = "3")
    Long commentCount,

    @Schema(description = "인기 점수 (engagement×10 + viewCount + engagement×1000/(viewCount+10))", example = "850")
    Long popularityScore,

    @Schema(description = "피드 생성 시각", example = "2026-03-02T14:30:00")
    LocalDateTime createdAt,

    @Schema(description = "현재 사용자가 이 Vibe를 아카이브했는지 여부 (비인증 시 false)")
    Boolean isArchived,

    @Schema(description = "아카이브 ID (아카이브하지 않은 경우 null)")
    Long archiveId
) {}
