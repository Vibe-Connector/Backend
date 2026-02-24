package com.link.vibe.global.common;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        String nextCursor,
        int size,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(List<T> fetchedContent, int size,
                                          Function<T, String> cursorExtractor) {
        boolean hasNext = fetchedContent.size() > size;
        List<T> content = hasNext ? fetchedContent.subList(0, size) : fetchedContent;
        String nextCursor = hasNext ? cursorExtractor.apply(content.get(content.size() - 1)) : null;
        return new PageResponse<>(content, nextCursor, size, hasNext);
    }
}
