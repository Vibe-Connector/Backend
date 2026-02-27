package com.link.vibe.domain.archive.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Vibe 아카이브 저장 요청")
public record ArchiveVibeRequest(

        @Schema(description = "Vibe 결과 ID", example = "1")
        @NotNull(message = "resultId는 필수입니다.")
        Long resultId,

        @Schema(description = "폴더 ID (선택, VIBE 타입 폴더만 가능)", example = "1")
        Long folderId,

        @Schema(description = "메모 (선택, 최대 500자)", example = "감성적인 분위기가 좋았다")
        @Size(max = 500, message = "메모는 500자 이하여야 합니다.")
        String memo
) {}
