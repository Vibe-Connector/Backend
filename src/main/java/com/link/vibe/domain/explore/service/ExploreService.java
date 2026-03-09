package com.link.vibe.domain.explore.service;

import com.link.vibe.domain.archive.entity.ArchiveVibe;
import com.link.vibe.domain.archive.repository.ArchiveVibeRepository;
import com.link.vibe.domain.explore.dto.ExplorePeriod;
import com.link.vibe.domain.explore.dto.ExploreVibeResponse;
import com.link.vibe.domain.feed.repository.FeedRepository;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExploreService {

    private final FeedRepository feedRepository;
    private final ArchiveVibeRepository archiveVibeRepository;
    private final S3StorageService s3StorageService;

    public PageResponse<ExploreVibeResponse> getPopularVibes(ExplorePeriod period,
                                                              CursorPageRequest request,
                                                              Long currentUserId) {
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

        // resultId 목록 추출 (native query column index 8)
        Set<Long> resultIds = results.stream()
            .map(row -> ((Number) row[8]).longValue())
            .collect(Collectors.toSet());

        // 인증된 사용자인 경우 아카이브 상태 조회
        Map<Long, Long> archivedMap = Map.of();
        if (currentUserId != null && !resultIds.isEmpty()) {
            List<ArchiveVibe> archives = archiveVibeRepository.findByUser(
                currentUserId, PageRequest.of(0, Integer.MAX_VALUE));
            archivedMap = archives.stream()
                .filter(av -> resultIds.contains(av.getVibeResult().getResultId()))
                .collect(Collectors.toMap(
                    av -> av.getVibeResult().getResultId(),
                    ArchiveVibe::getArchiveId
                ));
        }

        Map<Long, Long> finalArchivedMap = archivedMap;
        List<ExploreVibeResponse> content = results.stream()
            .map(row -> toExploreVibeResponse(row, finalArchivedMap))
            .toList();

        return PageResponse.of(content, request.getEffectiveSize(),
            item -> item.popularityScore() + "_" + item.feedId());
    }

    private ExploreVibeResponse toExploreVibeResponse(Object[] row, Map<Long, Long> archivedMap) {
        Long feedId = ((Number) row[0]).longValue();
        String caption = (String) row[1];
        Integer viewCount = ((Number) row[2]).intValue();
        LocalDateTime createdAt = ((Timestamp) row[3]).toLocalDateTime();
        Long authorId = ((Number) row[4]).longValue();
        String authorNickname = (String) row[5];
        String authorProfileImageUrl = (String) row[6];
        String generatedImageUrl = (String) row[7];
        Long resultId = ((Number) row[8]).longValue();
        Long reactionCount = ((Number) row[9]).longValue();
        Long commentCount = ((Number) row[10]).longValue();
        Long score = ((Number) row[11]).longValue();

        Long archiveId = archivedMap.getOrDefault(resultId, null);

        return new ExploreVibeResponse(
            feedId,
            resultId,
            generatedImageUrl != null ? s3StorageService.toPresignedUrl(generatedImageUrl) : null,
            caption,
            authorId,
            authorNickname,
            authorProfileImageUrl != null ? s3StorageService.toPresignedUrl(authorProfileImageUrl) : null,
            viewCount,
            reactionCount,
            commentCount,
            score,
            createdAt,
            archiveId != null,
            archiveId
        );
    }
}
