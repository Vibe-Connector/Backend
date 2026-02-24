package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "place_key", nullable = false, unique = true, length = 50)
    private String placeKey;

    @Column(name = "is_active")
    private Boolean isActive;
}
