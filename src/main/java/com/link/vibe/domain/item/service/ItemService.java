package com.link.vibe.domain.item.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.domain.item.dto.LightingDetailResponse;
import com.link.vibe.domain.item.dto.MovieDetailResponse;
import com.link.vibe.domain.item.dto.MusicDetailResponse;
import com.link.vibe.domain.item.entity.Item;
import com.link.vibe.domain.item.entity.LightingDetail;
import com.link.vibe.domain.item.entity.MovieDetail;
import com.link.vibe.domain.item.entity.MusicDetail;
import com.link.vibe.domain.item.repository.ItemCategoryTranslationRepository;
import com.link.vibe.domain.item.repository.ItemRepository;
import com.link.vibe.domain.item.repository.ItemTranslationRepository;
import com.link.vibe.domain.item.repository.LightingDetailRepository;
import com.link.vibe.domain.item.repository.MovieDetailRepository;
import com.link.vibe.domain.item.repository.MusicDetailRepository;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemTranslationRepository itemTranslationRepository;
    private final ItemCategoryTranslationRepository categoryTranslationRepository;
    private final MovieDetailRepository movieDetailRepository;
    private final MusicDetailRepository musicDetailRepository;
    private final LightingDetailRepository lightingDetailRepository;
    private final ObjectMapper objectMapper;

    public MovieDetailResponse getMovieDetail(Long itemId, Long languageId) {
        Item item = findActiveItem(itemId);
        MovieDetail detail = movieDetailRepository.findByItemId(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        String itemName = getItemName(itemId, languageId);
        String description = getItemDescription(itemId, languageId);
        String categoryName = getCategoryName(item.getCategory().getCategoryId(), languageId);

        return new MovieDetailResponse(
                item.getItemId(),
                itemName,
                description,
                item.getCategory().getCategoryKey(),
                categoryName,
                item.getBrand(),
                item.getImageUrl(),
                item.getExternalLink(),
                item.getExternalService(),
                detail.getTmdbId(),
                detail.getOriginalTitle(),
                detail.getOverview(),
                detail.getReleaseDate(),
                detail.getRuntime(),
                detail.getVoteAverage(),
                detail.getVoteCount(),
                detail.getPosterPath(),
                parseJsonArray(detail.getGenres(), new TypeReference<>() {}),
                parseJsonArray(detail.getCastInfo(), new TypeReference<>() {}),
                detail.getContentType()
        );
    }

    public MusicDetailResponse getMusicDetail(Long itemId, Long languageId) {
        Item item = findActiveItem(itemId);
        MusicDetail detail = musicDetailRepository.findByItemId(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        String itemName = getItemName(itemId, languageId);
        String description = getItemDescription(itemId, languageId);
        String categoryName = getCategoryName(item.getCategory().getCategoryId(), languageId);

        return new MusicDetailResponse(
                item.getItemId(),
                itemName,
                description,
                item.getCategory().getCategoryKey(),
                categoryName,
                item.getBrand(),
                item.getImageUrl(),
                item.getExternalLink(),
                item.getExternalService(),
                parseJsonArray(detail.getArtists(), new TypeReference<>() {}),
                detail.getAlbumName(),
                detail.getAlbumCoverUrl(),
                detail.getTrackDurationMs(),
                detail.getReleaseDate(),
                parseJsonArray(detail.getGenres(), new TypeReference<>() {}),
                detail.getPreviewUrl(),
                detail.getSpotifyUri(),
                detail.getContentType()
        );
    }

    public LightingDetailResponse getLightingDetail(Long itemId, Long languageId) {
        Item item = findActiveItem(itemId);
        LightingDetail detail = lightingDetailRepository.findByItemId(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        String itemName = getItemName(itemId, languageId);
        String description = getItemDescription(itemId, languageId);
        String categoryName = getCategoryName(item.getCategory().getCategoryId(), languageId);

        return new LightingDetailResponse(
                item.getItemId(),
                itemName,
                description,
                item.getCategory().getCategoryKey(),
                categoryName,
                item.getBrand(),
                item.getImageUrl(),
                item.getExternalLink(),
                item.getExternalService(),
                detail.getColorTempKelvin(),
                detail.getColorTempName(),
                detail.getBrightnessPercent(),
                detail.getBrightnessLevel(),
                detail.getLightingType(),
                detail.getLightColor(),
                detail.getPosition(),
                detail.getSpaceContext(),
                detail.getTimeContext(),
                detail.getIsDynamic()
        );
    }

    // ── private 헬퍼 ──

    private Item findActiveItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        if (Boolean.FALSE.equals(item.getIsActive())) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        return item;
    }

    private String getItemName(Long itemId, Long languageId) {
        if (languageId == null) return null;
        return itemTranslationRepository
                .findByItemItemIdAndLanguageLanguageId(itemId, languageId)
                .map(t -> t.getItemValue())
                .orElse(null);
    }

    private String getItemDescription(Long itemId, Long languageId) {
        if (languageId == null) return null;
        return itemTranslationRepository
                .findByItemItemIdAndLanguageLanguageId(itemId, languageId)
                .map(t -> t.getDescription())
                .orElse(null);
    }

    private String getCategoryName(Long categoryId, Long languageId) {
        if (languageId == null) return null;
        return categoryTranslationRepository
                .findByCategoryCategoryIdAndLanguageLanguageId(categoryId, languageId)
                .map(t -> t.getCategoryValue())
                .orElse(null);
    }

    private <T> List<T> parseJsonArray(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("JSON 배열 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
