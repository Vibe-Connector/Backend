package com.link.vibe.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lighting_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LightingDetail {

    @Id
    private Long itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(unique = true, nullable = false, length = 100)
    private String lightingKey;

    private Integer colorTempKelvin;

    @Column(nullable = false, length = 100)
    private String colorTempName;

    @Column(nullable = false)
    private Integer brightnessPercent;

    @Column(length = 50)
    private String brightnessLevel;

    @Column(nullable = false, length = 100)
    private String lightingType;

    @Column(length = 50)
    private String lightColor;

    @Column(length = 100)
    private String position;

    @Column(length = 255)
    private String spaceContext;

    @Column(length = 100)
    private String timeContext;

    private Boolean isDynamic;

    private Integer dynamicStartKelvin;

    private Integer dynamicEndKelvin;

    private Long linkedProductId;

    @Column(length = 100)
    private String hueSceneId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
