package com.link.vibe.domain.vibe.service.recommendation;

import java.math.BigDecimal;

public record RecommendationResult(
        Long itemId,
        String categoryKey,
        BigDecimal similarityScore,
        String itemKey,
        String brand,
        String imageUrl,
        String externalLink,
        String externalService
) {}
