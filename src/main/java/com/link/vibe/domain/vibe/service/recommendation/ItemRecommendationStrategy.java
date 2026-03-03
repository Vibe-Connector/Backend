package com.link.vibe.domain.vibe.service.recommendation;

import java.util.List;
import java.util.Map;

/**
 * 아이템 추천 전략 인터페이스.
 * 현재 구현: pgvector 코사인 유사도.
 * 향후 교체: GraphRAG 기반 추천.
 */
public interface ItemRecommendationStrategy {

    Map<String, List<RecommendationResult>> recommend(String moodDescription, int itemsPerCategory);
}
