package com.link.vibe.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "댓글 응답")
public record CommentResponse(
    @Schema(description = "댓글 ID") Long commentId,
    @Schema(description = "피드 ID") Long feedId,
    @Schema(description = "작성자 ID") Long userId,
    @Schema(description = "작성자 닉네임") String nickname,
    @Schema(description = "작성자 프로필 이미지") String profileImageUrl,
    @Schema(description = "부모 댓글 ID") Long parentCommentId,
    @Schema(description = "댓글 내용") String content,
    @Schema(description = "좋아요 수") Long likeCount,
    @Schema(description = "내가 좋아요 했는지") Boolean isLikedByMe,
    @Schema(description = "대댓글 목록") List<CommentResponse> replies,
    @Schema(description = "작성일시") LocalDateTime createdAt
) {}
