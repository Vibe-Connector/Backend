package com.link.vibe.domain.explore.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum ExplorePeriod {
    DAY(1),
    WEEK(7),
    MONTH(30);

    private final int days;

    public LocalDateTime toStartDateTime() {
        return LocalDateTime.now().minusDays(days);
    }
}
