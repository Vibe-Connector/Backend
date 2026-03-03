package com.link.vibe.domain.archive.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "폴더 생성 요청")
public record FolderCreateRequest(

        @Schema(description = "폴더명", example = "주말 감성")
        @NotBlank(message = "폴더명은 필수입니다.")
        @Size(max = 100, message = "폴더명은 100자 이하여야 합니다.")
        String folderName,

        @Schema(description = "폴더 타입 — VIBE: Vibe 결과 분류용, ITEM: 개별 아이템 분류용",
                example = "VIBE", allowableValues = {"VIBE", "ITEM"})
        @NotBlank(message = "폴더 타입은 필수입니다.")
        String folderType,

        @Schema(description = "썸네일 URL (선택)", example = "https://example.com/thumbnail.jpg")
        @Size(max = 500, message = "썸네일 URL은 500자 이하여야 합니다.")
        String thumbnailUrl,

        @Schema(description = "정렬 순서 (선택, 기본값 0)", example = "0")
        Integer sortOrder
) {}
