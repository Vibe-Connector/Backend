package com.link.vibe.domain.feed.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReactionType {
    LIKE("LIKE"),
    DISLIKE("DISLIKE"),
    WOW("WOW"),
    LOVE("LOVE");

    private final String value;
}
