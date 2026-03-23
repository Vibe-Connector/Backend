package com.link.vibe.domain.vibe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.domain.item.entity.MusicDetail;
import com.link.vibe.domain.item.repository.ItemRepository;
import com.link.vibe.domain.item.repository.ItemTranslationRepository;
import com.link.vibe.domain.item.repository.MusicDetailRepository;
import com.link.vibe.domain.option.entity.*;
import com.link.vibe.domain.option.repository.*;
import com.link.vibe.domain.vibe.dto.*;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerIngestSessionRequest;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerRecommendRequest;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerRecommendRequest.AIServerWeatherIntensity;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerRecommendResponse.AIServerCategoryRec;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerRecommendResponse.AIServerItem;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerRecommendResponse.AIServerRecommendData;
import com.link.vibe.domain.vibe.entity.*;
import com.link.vibe.domain.vibe.repository.*;
import com.link.vibe.global.event.VibeCompleteEvent;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import com.link.vibe.global.i18n.LanguageContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VibePromptService {

    private final VibeSessionRepository vibeSessionRepository;
    private final VibePromptRepository vibePromptRepository;
    private final VibeResultRepository vibeResultRepository;
    private final VibeItemRepository vibeItemRepository;
    private final ItemRepository itemRepository;
    private final MoodKeywordRepository moodKeywordRepository;
    private final TimeOptionRepository timeOptionRepository;
    private final WeatherOptionRepository weatherOptionRepository;
    private final PlaceOptionRepository placeOptionRepository;
    private final CompanionOptionRepository companionOptionRepository;
    private final ItemTranslationRepository itemTranslationRepository;
    private final MusicDetailRepository musicDetailRepository;
    private final AIServerClient aiServerClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${vibe.recommendation.items-per-category:3}")
    private int itemsPerCategory;

    @Transactional
    public VibePromptSubmitResponse submitPrompt(Long userId, Long sessionId,
                                                  VibeCreateRequest request) {
        // 1. 세션 검증 (소유권 + IN_PROGRESS 상태)
        VibeSession session = vibeSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIBE_SESSION_NOT_FOUND));

        if (!session.isInProgress()) {
            throw new BusinessException(ErrorCode.VIBE_SESSION_ALREADY_COMPLETED);
        }

        // 2. 옵션 검증
        List<MoodKeyword> moodKeywords = moodKeywordRepository.findAllById(request.moodKeywordIds());
        if (moodKeywords.size() != request.moodKeywordIds().size()) {
            throw new BusinessException(ErrorCode.VIBE_INVALID_KEYWORD);
        }

        TimeOption timeOption = timeOptionRepository.findById(request.timeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 시간 옵션입니다."));
        WeatherOption weatherOption = weatherOptionRepository.findById(request.weatherId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 날씨 옵션입니다."));
        PlaceOption placeOption = placeOptionRepository.findById(request.placeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 공간 옵션입니다."));
        CompanionOption companionOption = companionOptionRepository.findById(request.companionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 동반자 옵션입니다."));

        List<String> moodValues = moodKeywords.stream()
                .map(MoodKeyword::getKeywordValue).toList();

        // 3. 프롬프트 저장
        String promptDescription = buildPromptDescription(moodValues, timeOption, weatherOption,
                placeOption, companionOption, request);

        VibePrompt prompt = VibePrompt.builder()
                .vibeSession(session)
                .moodKeywordIds(toJson(request.moodKeywordIds()))
                .timeOption(timeOption)
                .weatherOption(weatherOption)
                .placeOption(placeOption)
                .companionOption(companionOption)
                .finalPrompt(promptDescription)
                .build();
        vibePromptRepository.save(prompt);

        // 4. AIServer 단일 호출 (phrase + analysis + 추천 아이템)
        long startTime = System.currentTimeMillis();

        List<AIServerWeatherIntensity> aiWeatherIntensities = request.weatherIntensities() != null
                ? request.weatherIntensities().stream()
                    .map(wi -> new AIServerWeatherIntensity(wi.weatherId().intValue(), wi.intensity()))
                    .toList()
                : List.of();

        AIServerRecommendRequest aiRequest = new AIServerRecommendRequest(
                request.moodKeywordIds().stream().map(Long::intValue).toList(),
                moodValues,
                timeOption.getTimeId().intValue(),
                timeOption.getTimeKey(),
                weatherOption.getWeatherId().intValue(),
                weatherOption.getWeatherKey(),
                placeOption.getPlaceId().intValue(),
                placeOption.getPlaceKey(),
                companionOption.getCompanionId().intValue(),
                companionOption.getCompanionKey(),
                request.hour(),
                request.minute(),
                aiWeatherIntensities,
                itemsPerCategory
        );

        AIServerRecommendData aiResponse = aiServerClient.recommend(aiRequest);
        int totalProcessingTimeMs = (int) (System.currentTimeMillis() - startTime);

        // 5. VibeResult 저장
        VibeResult result = VibeResult.builder()
                .vibeSession(session)
                .phrase(aiResponse.phrase())
                .aiAnalysis(aiResponse.analysis())
                .aiModelVersion("graphrag")
                .processingTimeMs(totalProcessingTimeMs)
                .build();
        vibeResultRepository.save(result);

        // 6. VibeItem 저장 (recommendReason 포함)
        List<VibeItem> vibeItems = new ArrayList<>();
        List<Integer> recommendedItemIds = new ArrayList<>();

        for (AIServerCategoryRec catRec : aiResponse.recommendations()) {
            for (AIServerItem item : catRec.items()) {
                VibeItem vibeItem = VibeItem.builder()
                        .vibeResult(result)
                        .item(itemRepository.getReferenceById((long) item.itemId()))
                        .matchScore(BigDecimal.valueOf(item.relevanceScore()))
                        .recommendReason(item.reason())
                        .build();
                vibeItems.add(vibeItem);
                recommendedItemIds.add(item.itemId());
            }
        }
        vibeItemRepository.saveAll(vibeItems);

        // 7. 세션 완료 처리
        session.complete();

        // 8. 이벤트 발행
        eventPublisher.publishEvent(new VibeCompleteEvent(sessionId, userId));

        // 9. 비동기 학습 데이터 전송
        aiServerClient.ingestSession(new AIServerIngestSessionRequest(
                sessionId,
                userId,
                moodValues,
                request.moodKeywordIds().stream().map(Long::intValue).toList(),
                timeOption.getTimeKey(),
                timeOption.getTimeId().intValue(),
                weatherOption.getWeatherKey(),
                weatherOption.getWeatherId().intValue(),
                placeOption.getPlaceKey(),
                placeOption.getPlaceId().intValue(),
                companionOption.getCompanionKey(),
                companionOption.getCompanionId().intValue(),
                aiResponse.phrase(),
                aiResponse.analysis(),
                recommendedItemIds
        ));

        // 10. 응답 구성
        Long languageId = LanguageContext.getLanguageId();

        List<CategoryRecommendation> categoryRecs = aiResponse.recommendations().stream()
                .map(catRec -> new CategoryRecommendation(
                        catRec.categoryKey(),
                        catRec.items().stream()
                                .map(item -> {
                                    // 음악 카테고리인 경우 MusicDetail 데이터 포함
                                    String albumCoverUrl = null;
                                    String previewUrl = null;
                                    String spotifyUri = null;
                                    String isrc = null;
                                    String musicbrainzId = null;

                                    if ("music".equals(catRec.categoryKey())) {
                                        Optional<MusicDetail> musicDetailOpt = musicDetailRepository.findByItemId((long) item.itemId());
                                        if (musicDetailOpt.isPresent()) {
                                            MusicDetail md = musicDetailOpt.get();
                                            albumCoverUrl = md.getAlbumCoverUrl();
                                            previewUrl = md.getPreviewUrl();
                                            spotifyUri = md.getSpotifyUri();
                                            isrc = md.getIsrc();
                                            musicbrainzId = md.getMusicbrainzId();
                                        }
                                    }

                                    // 번역된 이름 → AI서버 name → itemKey 순서로 폴백
                                    String resolvedName = getItemName((long) item.itemId(), languageId);
                                    if (resolvedName == null) {
                                        resolvedName = item.name();
                                    }

                                    return new RecommendedItemResponse(
                                            (long) item.itemId(),
                                            item.itemKey(),
                                            resolvedName,
                                            catRec.categoryKey(),
                                            item.brand(),
                                            item.imageUrl(),
                                            item.externalLink(),
                                            item.externalService(),
                                            BigDecimal.valueOf(item.relevanceScore()),
                                            item.reason(),
                                            albumCoverUrl,
                                            previewUrl,
                                            spotifyUri,
                                            isrc,
                                            musicbrainzId
                                    );
                                })
                                .toList()
                ))
                .toList();

        VibeResultResponse.SelectedOptions selectedOptions = new VibeResultResponse.SelectedOptions(
                moodValues,
                timeOption.getTimeKey(),
                weatherOption.getWeatherKey(),
                placeOption.getPlaceKey(),
                companionOption.getCompanionKey()
        );

        return new VibePromptSubmitResponse(
                sessionId,
                aiResponse.phrase(),
                aiResponse.analysis(),
                null,
                selectedOptions,
                categoryRecs,
                totalProcessingTimeMs,
                session.getCreatedAt()
        );
    }

    private String buildPromptDescription(List<String> moodValues, TimeOption timeOption,
                                           WeatherOption weatherOption, PlaceOption placeOption,
                                           CompanionOption companionOption, VibeCreateRequest request) {
        String timeDescription = timeOption.getTimeKey();
        if (request.hour() != null) {
            String minuteStr = request.minute() != null ? String.format("%02d", request.minute()) : "00";
            timeDescription = timeOption.getTimeKey() + " (" + request.hour() + ":" + minuteStr + ")";
        }

        String weatherDescription = weatherOption.getWeatherKey();
        if (request.weatherIntensities() != null && !request.weatherIntensities().isEmpty()) {
            List<String> weatherParts = request.weatherIntensities().stream()
                    .filter(wi -> wi.intensity() > 0)
                    .map(wi -> {
                        WeatherOption wo = weatherOptionRepository.findById(wi.weatherId()).orElse(null);
                        if (wo == null) return null;
                        return wo.getWeatherKey() + " " + wi.intensity() + "%";
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (!weatherParts.isEmpty()) {
                weatherDescription = String.join(", ", weatherParts);
            }
        }

        return String.join(" | ", moodValues) + " / " + timeDescription + " / "
                + weatherDescription + " / " + placeOption.getPlaceKey() + " / "
                + companionOption.getCompanionKey();
    }

    private String getItemName(Long itemId, Long languageId) {
        if (languageId == null) return null;
        return itemTranslationRepository
                .findByItemItemIdAndLanguageLanguageId(itemId, languageId)
                .map(t -> t.getItemValue())
                .orElse(null);
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "JSON 변환 실패");
        }
    }
}
