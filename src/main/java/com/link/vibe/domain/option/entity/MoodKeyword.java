package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mood_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoodKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long keywordId;

    @Column(name = "keyword_value", nullable = false, unique = true, length = 50)
    private String keywordValue;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
