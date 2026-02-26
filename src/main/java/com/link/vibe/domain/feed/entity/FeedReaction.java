package com.link.vibe.domain.feed.entity;

import com.link.vibe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_reactions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_feed_user_reaction",
        columnNames = {"feed_id", "user_id", "reaction_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long reactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 20)
    private ReactionType reactionType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static FeedReaction create(Feed feed, User user, ReactionType reactionType) {
        FeedReaction reaction = new FeedReaction();
        reaction.feed = feed;
        reaction.user = user;
        reaction.reactionType = reactionType;
        return reaction;
    }
}
