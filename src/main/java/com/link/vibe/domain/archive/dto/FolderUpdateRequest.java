package com.link.vibe.domain.archive.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "폴더 수정 요청 — folderType은 변경 불가")
public record FolderUpdateRequest(

        @Schema(description = "폴더명 (변경할 경우에만 전송)", example = "평일 감성")
        @Size(max = 100, message = "폴더명은 100자 이하여야 합니다.")
        String folderName,

        @Schema(description = "썸네일 URL (변경할 경우에만 전송)", example = "https://example.com/new-thumbnail.jpg")
        @Size(max = 500, message = "썸네일 URL은 500자 이하여야 합니다.")
        String thumbnailUrl,

        @Schema(description = "정렬 순서 (변경할 경우에만 전송)", example = "1")
        Integer sortOrder
) {}
