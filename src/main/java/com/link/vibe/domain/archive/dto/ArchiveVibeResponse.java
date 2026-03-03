package com.link.vibe.domain.archive.dto;

import com.link.vibe.domain.archive.entity.ArchiveFolder;
import com.link.vibe.domain.archive.entity.ArchiveVibe;
import com.link.vibe.domain.vibe.entity.VibeResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Vibe 아카이브 응답")
public record ArchiveVibeResponse(

        @Schema(description = "아카이브 ID", example = "1")
        Long archiveId,

        @Schema(description = "Vibe 결과 ID", example = "10")
        Long resultId,

        @Schema(description = "세션 ID", example = "5")
        Long sessionId,

        @Schema(description = "AI 생성 문구", example = "따뜻한 오후, 커피 한 잔과 함께하는 감성")
        String phrase,

        @Schema(description = "생성 이미지 URL", example = "https://example.com/image.jpg")
        String generatedImageUrl,

        @Schema(description = "폴더 ID (미분류 시 null)", example = "1")
        Long folderId,

        @Schema(description = "폴더명 (미분류 시 null)", example = "주말 감성")
        String folderName,

        @Schema(description = "메모", example = "감성적인 분위기가 좋았다")
        String memo,

        @Schema(description = "즐겨찾기 여부", example = "false")
        boolean isFavorite,

        @Schema(description = "아카이브 저장 시각", example = "2026-02-27T10:30:00")
        LocalDateTime createdAt
) {
    public static ArchiveVibeResponse of(ArchiveVibe archiveVibe, boolean isFavorite) {
        VibeResult result = archiveVibe.getVibeResult();
        ArchiveFolder folder = archiveVibe.getFolder();
        return new ArchiveVibeResponse(
                archiveVibe.getArchiveId(),
                result.getResultId(),
                result.getVibeSession().getSessionId(),
                result.getPhrase(),
                result.getGeneratedImageUrl(),
                folder != null ? folder.getFolderId() : null,
                folder != null ? folder.getFolderName() : null,
                archiveVibe.getMemo(),
                isFavorite,
                archiveVibe.getCreatedAt()
        );
    }
}
