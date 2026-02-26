package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mood_keyword_translations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoodKeywordTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long translationId;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(name = "language_id", nullable = false)
    private Long languageId;

    @Column(name = "keyword_value", nullable = false, length = 100)
    private String keywordValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
