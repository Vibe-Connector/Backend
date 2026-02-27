package com.link.vibe.domain.archive.dto;

import com.link.vibe.domain.archive.entity.ArchiveFolder;
import com.link.vibe.domain.archive.entity.ArchiveItem;
import com.link.vibe.domain.item.entity.Item;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "아이템 아카이브 응답")
public record ArchiveItemResponse(

        @Schema(description = "아카이브 아이템 ID", example = "1")
        Long archiveItemId,

        @Schema(description = "아이템 ID", example = "10")
        Long itemId,

        @Schema(description = "아이템 이름 (i18n)", example = "라떼")
        String itemName,

        @Schema(description = "카테고리 키", example = "coffee")
        String categoryKey,

        @Schema(description = "브랜드", example = "스타벅스")
        String brand,

        @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
        String imageUrl,

        @Schema(description = "폴더 ID (미분류 시 null)", example = "1")
        Long folderId,

        @Schema(description = "폴더명 (미분류 시 null)", example = "나만의 커피 리스트")
        String folderName,

        @Schema(description = "메모", example = "이 커피 꼭 마셔봐야지")
        String memo,

        @Schema(description = "즐겨찾기 여부", example = "false")
        boolean isFavorite,

        @Schema(description = "아카이브 저장 시각", example = "2026-02-27T10:30:00")
        LocalDateTime createdAt
) {
    public static ArchiveItemResponse of(ArchiveItem archiveItem,
                                          String itemName, String categoryKey,
                                          boolean isFavorite) {
        Item item = archiveItem.getItem();
        ArchiveFolder folder = archiveItem.getFolder();
        return new ArchiveItemResponse(
                archiveItem.getArchiveItemId(),
                item.getItemId(),
                itemName,
                categoryKey,
                item.getBrand(),
                item.getImageUrl(),
                folder != null ? folder.getFolderId() : null,
                folder != null ? folder.getFolderName() : null,
                archiveItem.getMemo(),
                isFavorite,
                archiveItem.getCreatedAt()
        );
    }
}
