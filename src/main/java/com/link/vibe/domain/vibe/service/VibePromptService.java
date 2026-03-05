package com.link.vibe.domain.vibe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.domain.item.repository.ItemRepository;
import com.link.vibe.domain.item.repository.ItemTranslationRepository;
import com.link.vibe.domain.option.entity.*;
import com.link.vibe.domain.option.repository.*;
import com.link.vibe.domain.vibe.dto.*;
import com.link.vibe.domain.vibe.entity.*;
import com.link.vibe.domain.vibe.repository.*;
import com.link.vibe.domain.vibe.service.recommendation.*;
import com.link.vibe.global.event.VibeCompleteEvent;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import com.link.vibe.global.i18n.LanguageContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final OpenAiService openAiService;
    private final ItemRecommendationStrategy recommendationStrategy;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${openai.model:gpt-4o-mini}")
    private String aiModel;

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

        // 3. 프롬프트 저장 — hour/minute, weatherIntensities 반영
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

        String finalPrompt = openAiService.buildUserPrompt(
                moodValues, timeDescription, weatherDescription,
                placeOption.getPlaceKey(), companionOption.getCompanionKey());

        VibePrompt prompt = VibePrompt.builder()
                .vibeSession(session)
                .moodKeywordIds(toJson(request.moodKeywordIds()))
                .timeOption(timeOption)
                .weatherOption(weatherOption)
                .placeOption(placeOption)
                .companionOption(companionOption)
                .finalPrompt(finalPrompt)
                .build();
        vibePromptRepository.save(prompt);

        // 4. OpenAI Chat API 호출 (분위기 문구 + 분석)
        long startTime = System.currentTimeMillis();
        OpenAiService.VibeResult aiResult = openAiService.generateVibe(
                moodValues, timeDescription, weatherDescription,
                placeOption.getPlaceKey(), companionOption.getCompanionKey());

        // 5. 아이템 추천 (pgvector 유사도 검색)
        Map<String, List<RecommendationResult>> recommendations =
                recommendationStrategy.recommend(finalPrompt, itemsPerCategory);
        int totalProcessingTimeMs = (int) (System.currentTimeMillis() - startTime);

        // 6. VibeResult 저장
        VibeResult result = VibeResult.builder()
                .vibeSession(session)
                .phrase(aiResult.phrase())
                .aiAnalysis(aiResult.analysis())
                .aiModelVersion(aiModel)
                .processingTimeMs(totalProcessingTimeMs)
                .build();
        vibeResultRepository.save(result);

        // 7. VibeItem 저장
        List<VibeItem> vibeItems = new ArrayList<>();
        for (Map.Entry<String, List<RecommendationResult>> entry : recommendations.entrySet()) {
            for (RecommendationResult rec : entry.getValue()) {
                VibeItem vibeItem = VibeItem.builder()
                        .vibeResult(result)
                        .item(itemRepository.getReferenceById(rec.itemId()))
                        .matchScore(rec.similarityScore())
                        .build();
                vibeItems.add(vibeItem);
            }
        }
        vibeItemRepository.saveAll(vibeItems);

        // 8. 세션 완료 처리
        session.complete();

        // 9. 이벤트 발행
        eventPublisher.publishEvent(new VibeCompleteEvent(sessionId, userId));

        // 10. 응답 구성
        Long languageId = LanguageContext.getLanguageId();

        List<CategoryRecommendation> categoryRecs = recommendations.entrySet().stream()
                .map(entry -> new CategoryRecommendation(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(rec -> new RecommendedItemResponse(
                                        rec.itemId(),
                                        rec.itemKey(),
                                        getItemName(rec.itemId(), languageId),
                                        rec.categoryKey(),
                                        rec.brand(),
                                        rec.imageUrl(),
                                        rec.externalLink(),
                                        rec.externalService(),
                                        rec.similarityScore(),
                                        null
                                ))
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
                aiResult.phrase(),
                aiResult.analysis(),
                null,
                selectedOptions,
                categoryRecs,
                totalProcessingTimeMs,
                session.getCreatedAt()
        );
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
