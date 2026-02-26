package com.link.vibe.domain.feed.entity;

import com.link.vibe.domain.user.entity.User;
import com.link.vibe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class FeedComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private FeedComment parentComment;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_hidden")
    private Boolean isHidden;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static FeedComment create(Feed feed, User user, FeedComment parentComment, String content) {
        FeedComment comment = new FeedComment();
        comment.feed = feed;
        comment.user = user;
        comment.parentComment = parentComment;
        comment.content = content;
        comment.isHidden = false;
        return comment;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isOwner(Long userId) {
        return this.user.getUserId().equals(userId);
    }

    public boolean isReply() {
        return this.parentComment != null;
    }
}
