package com.link.vibe.domain.archive.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "아이템 아카이브 저장 요청")
public record ArchiveItemRequest(

        @Schema(description = "아이템 ID", example = "1")
        @NotNull(message = "itemId는 필수입니다.")
        Long itemId,

        @Schema(description = "폴더 ID (선택, ITEM 타입 폴더만 가능)", example = "1")
        Long folderId,

        @Schema(description = "반응 ID (선택)", example = "1")
        Long reactionId,

        @Schema(description = "메모 (선택, 최대 500자)", example = "이 커피 꼭 마셔봐야지")
        @Size(max = 500, message = "메모는 500자 이하여야 합니다.")
        String memo
) {}
