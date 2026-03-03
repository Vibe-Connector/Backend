package com.link.vibe.domain.explore.service;

import com.link.vibe.domain.explore.dto.ExplorePeriod;
import com.link.vibe.domain.explore.dto.ExploreVibeResponse;
import com.link.vibe.domain.feed.repository.FeedRepository;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExploreService {

    private final FeedRepository feedRepository;

    public PageResponse<ExploreVibeResponse> getPopularVibes(ExplorePeriod period,
                                                              CursorPageRequest request) {
        LocalDateTime since = period.toStartDateTime();

        Long cursorScore = Long.MAX_VALUE;
        Long cursorId = Long.MAX_VALUE;
        if (request.hasCursor()) {
            String[] parts = request.getCursor().split("_");
            cursorScore = Long.parseLong(parts[0]);
            cursorId = Long.parseLong(parts[1]);
        }

        List<Object[]> results = feedRepository.findPopularFeeds(
            since, cursorScore, cursorId,
            PageRequest.of(0, request.getFetchSize())
        );

        List<ExploreVibeResponse> content = results.stream()
            .map(this::toExploreVibeResponse)
            .toList();

        return PageResponse.of(content, request.getEffectiveSize(),
            item -> item.popularityScore() + "_" + item.feedId());
    }

    private ExploreVibeResponse toExploreVibeResponse(Object[] row) {
        Long feedId = ((Number) row[0]).longValue();
        String caption = (String) row[1];
        Integer viewCount = ((Number) row[2]).intValue();
        LocalDateTime createdAt = ((Timestamp) row[3]).toLocalDateTime();
        Long authorId = ((Number) row[4]).longValue();
        String authorNickname = (String) row[5];
        String authorProfileImageUrl = (String) row[6];
        String generatedImageUrl = (String) row[7];
        Long reactionCount = ((Number) row[8]).longValue();
        Long commentCount = ((Number) row[9]).longValue();
        Long score = ((Number) row[10]).longValue();

        return new ExploreVibeResponse(
            feedId,
            generatedImageUrl,
            caption,
            authorId,
            authorNickname,
            authorProfileImageUrl,
            viewCount,
            reactionCount,
            commentCount,
            score,
            createdAt
        );
    }
}
