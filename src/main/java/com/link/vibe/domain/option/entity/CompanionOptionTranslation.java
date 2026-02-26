package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "companion_option_translations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionOptionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long translationId;

    @Column(name = "companion_id", nullable = false)
    private Long companionId;

    @Column(name = "language_id", nullable = false)
    private Long languageId;

    @Column(name = "companion_value", nullable = false, length = 100)
    private String companionValue;
}
