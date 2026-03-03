package com.link.vibe.domain.item.service;

import com.link.vibe.domain.item.entity.*;
import com.link.vibe.domain.item.repository.*;
import com.link.vibe.domain.vibe.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemEmbeddingBatchService {

    private final ItemRepository itemRepository;
    private final ItemVectorRepository itemVectorRepository;
    private final MovieDetailRepository movieDetailRepository;
    private final MusicDetailRepository musicDetailRepository;
    private final LightingDetailRepository lightingDetailRepository;
    private final CoffeeDetailRepository coffeeDetailRepository;
    private final EmbeddingService embeddingService;

    @Transactional
    public int generateAllEmbeddings() {
        List<Item> items = itemRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .toList();

        int successCount = 0;

        for (Item item : items) {
            try {
                String text = buildDescriptionText(item);
                if (text == null || text.isBlank()) {
                    log.warn("아이템 {} 설명 텍스트 생성 실패, 건너뜀", item.getItemId());
                    continue;
                }

                float[] embedding = embeddingService.generateEmbedding(text);
                String vectorString = embeddingService.toVectorString(embedding);
                itemVectorRepository.updateEmbedding(item.getItemId(), vectorString);
                successCount++;

                log.info("아이템 {} 임베딩 생성 완료 ({}/{})", item.getItemId(), successCount, items.size());

                // OpenAI API rate limit 방지
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("아이템 {} 임베딩 생성 실패: {}", item.getItemId(), e.getMessage());
            }
        }

        log.info("임베딩 배치 완료: 총 {} / {} 건 성공", successCount, items.size());
        return successCount;
    }

    private String buildDescriptionText(Item item) {
        String categoryKey = item.getCategory().getCategoryKey();

        return switch (categoryKey) {
            case "movie", "video" -> buildMovieText(item.getItemId());
            case "music" -> buildMusicText(item.getItemId());
            case "lighting" -> buildLightingText(item.getItemId());
            case "coffee" -> buildCoffeeText(item.getItemId());
            default -> item.getItemKey();
        };
    }

    private String buildMovieText(Long itemId) {
        return movieDetailRepository.findByItemId(itemId)
                .map(d -> String.format("%s. %s. Genres: %s. Keywords: %s",
                        nullSafe(d.getOriginalTitle()),
                        nullSafe(d.getOverview()),
                        nullSafe(d.getGenres()),
                        nullSafe(d.getKeywords())))
                .orElse(null);
    }

    private String buildMusicText(Long itemId) {
        return musicDetailRepository.findByItemId(itemId)
                .map(d -> String.format("%s - %s. Genres: %s",
                        nullSafe(d.getArtists()),
                        nullSafe(d.getAlbumName()),
                        nullSafe(d.getGenres())))
                .orElse(null);
    }

    private String buildLightingText(Long itemId) {
        return lightingDetailRepository.findByItemId(itemId)
                .map(d -> String.format("%s %s %s %s %s",
                        nullSafe(d.getLightingType()),
                        nullSafe(d.getColorTempName()),
                        nullSafe(d.getSpaceContext()),
                        nullSafe(d.getTimeContext()),
                        nullSafe(d.getLightColor())))
                .orElse(null);
    }

    private String buildCoffeeText(Long itemId) {
        return coffeeDetailRepository.findByItemId(itemId)
                .map(d -> String.format("%s %s. Aroma: %s. Flavor: %s. Roast: %s. Intensity: %s",
                        nullSafe(d.getCapsuleName()),
                        nullSafe(d.getLine()),
                        nullSafe(d.getAromaProfile()),
                        nullSafe(d.getFlavorNotes()),
                        nullSafe(d.getRoastLevel()),
                        d.getIntensity() != null ? d.getIntensity().toString() : ""))
                .orElse(null);
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
