package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "companion_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "companion_id")
    private Long companionId;

    @Column(name = "companion_key", nullable = false, unique = true, length = 50)
    private String companionKey;

    @Column(name = "companion_text", nullable = false, length = 100)
    private String companionText;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;
}
