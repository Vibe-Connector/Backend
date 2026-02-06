package com.link.vibe.domain.option.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "time_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_id")
    private Long timeId;

    @Column(name = "time_key", nullable = false, unique = true, length = 50)
    private String timeKey;

    @Column(name = "time_text", nullable = false, length = 100)
    private String timeText;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;
}
