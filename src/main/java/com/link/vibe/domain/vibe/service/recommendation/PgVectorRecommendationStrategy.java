package com.link.vibe.domain.vibe.service.recommendation;

import com.link.vibe.domain.item.entity.ItemCategory;
import com.link.vibe.domain.item.repository.ItemCategoryRepository;
import com.link.vibe.domain.item.repository.ItemVectorRepository;
import com.link.vibe.domain.vibe.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PgVectorRecommendationStrategy implements ItemRecommendationStrategy {

    private final EmbeddingService embeddingService;
    private final ItemVectorRepository itemVectorRepository;
    private final ItemCategoryRepository itemCategoryRepository;

    @Override
    public Map<String, List<RecommendationResult>> recommend(String moodDescription, int itemsPerCategory) {
        float[] queryEmbedding = embeddingService.generateEmbedding(moodDescription);
        String vectorString = embeddingService.toVectorString(queryEmbedding);

        List<ItemCategory> categories = itemCategoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .toList();

        Map<String, List<RecommendationResult>> results = new LinkedHashMap<>();

        for (ItemCategory category : categories) {
            try {
                List<Object[]> rows = itemVectorRepository.findSimilarItemsByCategory(
                        category.getCategoryId(), vectorString, itemsPerCategory);

                List<RecommendationResult> categoryResults = rows.stream()
                        .map(row -> new RecommendationResult(
                                ((Number) row[0]).longValue(),
                                category.getCategoryKey(),
                                row[8] != null ? new BigDecimal(row[8].toString()) : BigDecimal.ZERO,
                                (String) row[2],
                                (String) row[3],
                                (String) row[4],
                                (String) row[5],
                                (String) row[6]
                        ))
                        .toList();

                results.put(category.getCategoryKey(), categoryResults);
            } catch (Exception e) {
                log.warn("카테고리 {} 유사도 검색 실패: {}", category.getCategoryKey(), e.getMessage());
                results.put(category.getCategoryKey(), Collections.emptyList());
            }
        }

        return results;
    }
}
