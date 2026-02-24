package com.link.vibe.global.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursorPageRequest {

    private String cursor;
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
