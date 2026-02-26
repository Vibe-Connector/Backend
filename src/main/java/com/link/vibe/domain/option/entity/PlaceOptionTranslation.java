package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place_option_translations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceOptionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long translationId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "language_id", nullable = false)
    private Long languageId;

    @Column(name = "place_value", nullable = false, length = 100)
    private String placeValue;
}
