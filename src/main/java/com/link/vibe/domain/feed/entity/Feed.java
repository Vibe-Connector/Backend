package com.link.vibe.domain.feed.entity;

import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "feeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Feed extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_id")
    private Long feedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private VibeResult vibeResult;

    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "is_pinned")
    private Boolean isPinned;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Feed create(User user, VibeResult vibeResult, String caption, Boolean isPublic) {
        Feed feed = new Feed();
        feed.user = user;
        feed.vibeResult = vibeResult;
        feed.caption = caption;
        feed.isPublic = (isPublic != null) ? isPublic : false;
        feed.isPinned = false;
        feed.viewCount = 0;
        return feed;
    }

    public void update(String caption, Boolean isPublic) {
        if (caption != null) this.caption = caption;
        if (isPublic != null) this.isPublic = isPublic;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public boolean isOwner(Long userId) {
        return this.user.getUserId().equals(userId);
    }
}
