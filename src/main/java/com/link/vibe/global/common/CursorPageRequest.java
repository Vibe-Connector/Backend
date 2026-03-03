package com.link.vibe.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursorPageRequest {

    @Schema(description = "다음 페이지 커서 (첫 페이지는 미전달)", example = "42")
    private String cursor;

    @Schema(description = "페이지 크기 (기본값 20, 최대 100)", example = "20")
    private int size = 20;

    public static final int MAX_SIZE = 100;

    public int getEffectiveSize() {
        return Math.min(size, MAX_SIZE);
    }

    public int getFetchSize() {
        return getEffectiveSize() + 1;
    }

    public boolean hasCursor() {
        return cursor != null && !cursor.isBlank();
    }
}
