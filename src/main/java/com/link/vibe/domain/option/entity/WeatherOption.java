package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weather_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weather_id")
    private Long weatherId;

    @Column(name = "weather_key", nullable = false, unique = true, length = 50)
    private String weatherKey;

    @Column(name = "weather_text", nullable = false, length = 100)
    private String weatherText;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;
}
