package com.link.vibe.domain.item.entity;

import com.link.vibe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    @Column(unique = true, nullable = false, length = 100)
    private String itemKey;

    @Column(length = 100)
    private String brand;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String externalLink;

    @Column(length = 50)
    private String externalService;

    private Boolean isActive;
}
