package com.link.vibe.domain.feed.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReactionType {
    LIKE("LIKE"),
    LOVE("LOVE"),
    WOW("WOW"),
    COZY("COZY");

    private final String value;
}
