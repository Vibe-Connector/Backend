package com.link.vibe.domain.archive.dto;

import com.link.vibe.domain.archive.entity.ArchiveFolder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "폴더 응답")
public record FolderResponse(

        @Schema(description = "폴더 ID", example = "1")
        Long folderId,

        @Schema(description = "폴더명", example = "주말 감성")
        String folderName,

        @Schema(description = "폴더 타입 (VIBE: Vibe 결과용, ITEM: 개별 아이템용)", example = "VIBE")
        String folderType,

        @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
        String thumbnailUrl,

        @Schema(description = "정렬 순서", example = "0")
        Integer sortOrder,

        @Schema(description = "폴더 내 아카이브 수", example = "5")
        long archiveCount,

        @Schema(description = "생성 시각", example = "2026-02-27T10:30:00")
        LocalDateTime createdAt
) {
    public static FolderResponse of(ArchiveFolder folder, long archiveCount) {
        return new FolderResponse(
                folder.getFolderId(),
                folder.getFolderName(),
                folder.getFolderType(),
                folder.getThumbnailUrl(),
                folder.getSortOrder(),
                archiveCount,
                folder.getCreatedAt()
        );
    }
}
