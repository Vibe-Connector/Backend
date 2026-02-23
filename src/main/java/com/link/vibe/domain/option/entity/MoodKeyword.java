package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mood_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoodKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long keywordId;

    @Column(name = "keyword_key", nullable = false, unique = true, length = 50)
    private String keywordKey;

    @Column(name = "keyword_text", nullable = false, length = 100)
    private String keywordText;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;
}
