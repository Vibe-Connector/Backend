package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_option_translations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherOptionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long translationId;

    @Column(name = "weather_id", nullable = false)
    private Long weatherId;

    @Column(name = "language_id", nullable = false)
    private Long languageId;

    @Column(name = "weather_value", nullable = false, length = 100)
    private String weatherValue;
}
