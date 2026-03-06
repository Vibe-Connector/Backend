package com.link.vibe.domain.vibe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.domain.option.entity.*;
import com.link.vibe.domain.option.repository.*;
import com.link.vibe.domain.item.repository.ItemTranslationRepository;
import com.link.vibe.domain.vibe.dto.*;
import com.link.vibe.domain.vibe.dto.VibeResultResponse.SelectedOptions;
import com.link.vibe.domain.vibe.entity.VibeItem;
import com.link.vibe.domain.vibe.entity.VibePrompt;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibeItemRepository;
import com.link.vibe.domain.vibe.repository.VibePromptRepository;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
import com.link.vibe.global.i18n.LanguageContext;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import com.link.vibe.global.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VibeService {

    private final VibeSessionRepository vibeSessionRepository;
    private final VibePromptRepository vibePromptRepository;
    private final VibeResultRepository vibeResultRepository;
    private final VibeItemRepository vibeItemRepository;
    private final ItemTranslationRepository itemTranslationRepository;
    private final MoodKeywordRepository moodKeywordRepository;
    private final TimeOptionRepository timeOptionRepository;
    private final WeatherOptionRepository weatherOptionRepository;
    private final PlaceOptionRepository placeOptionRepository;
    private final CompanionOptionRepository companionOptionRepository;
    private final S3StorageService s3StorageService;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    @Value("${openai.model:gpt-4o-mini}")
    private String aiModel;

    @Transactional
    public VibeSessionCreateResponse createSession(Long userId) {
        VibeSession session = VibeSession.builder().userId(userId).build();
        vibeSessionRepository.save(session);

        return new VibeSessionCreateResponse(
                session.getSessionId(),
                session.getStatus(),
                session.getCreatedAt()
        );
    }

    @Transactional
    public VibeResultResponse createVibe(Long userId, VibeCreateRequest request) {
        // 옵션 검증
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

        // 1. 세션 생성
        VibeSession session = VibeSession.builder().userId(userId).build();
        vibeSessionRepository.save(session);

        // 2. OpenAI 호출 — 시간/날씨 상세 정보 반영
        List<String> moodValues = moodKeywords.stream().map(MoodKeyword::getKeywordValue).toList();

        // hour/minute가 있으면 정밀 시간 문자열 생성
        String timeDescription = timeOption.getTimeKey();
        if (request.hour() != null) {
            String minuteStr = request.minute() != null ? String.format("%02d", request.minute()) : "00";
            timeDescription = timeOption.getTimeKey() + " (" + request.hour() + ":" + minuteStr + ")";
        }

        // weatherIntensities가 있으면 날씨 조합 문자열 생성
        String weatherDescription = weatherOption.getWeatherKey();
        if (request.weatherIntensities() != null && !request.weatherIntensities().isEmpty()) {
            List<String> weatherParts = request.weatherIntensities().stream()
                    .filter(wi -> wi.intensity() > 0)
                    .map(wi -> {
                        WeatherOption wo = weatherOptionRepository.findById(wi.weatherId()).orElse(null);
                        if (wo == null) return null;
                        return wo.getWeatherKey() + " " + wi.intensity() + "%";
                    })
                    .filter(Objects::nonNull)
                    .toList();
            if (!weatherParts.isEmpty()) {
                weatherDescription = String.join(", ", weatherParts);
            }
        }

        long startTime = System.currentTimeMillis();
        OpenAiService.VibeResult aiResult = openAiService.generateVibe(
                moodValues,
                timeDescription,
                weatherDescription,
                placeOption.getPlaceKey(),
                companionOption.getCompanionKey()
        );
        int processingTimeMs = (int) (System.currentTimeMillis() - startTime);

        String finalPrompt = openAiService.buildUserPrompt(
                moodValues,
                timeDescription,
                weatherDescription,
                placeOption.getPlaceKey(),
                companionOption.getCompanionKey()
        );

        // 3. 프롬프트 저장
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

        // 4. 결과 저장
        VibeResult result = VibeResult.builder()
                .vibeSession(session)
                .phrase(aiResult.phrase())
                .aiAnalysis(aiResult.analysis())
                .aiModelVersion(aiModel)
                .processingTimeMs(processingTimeMs)
                .build();
        vibeResultRepository.save(result);

        // 5. 세션 완료 처리
        session.complete();

        return new VibeResultResponse(
                session.getSessionId(),
                result.getResultId(),
                aiResult.phrase(),
                aiResult.analysis(),
                null,
                new SelectedOptions(
                        moodValues,
                        timeOption.getTimeKey(),
                        weatherOption.getWeatherKey(),
                        placeOption.getPlaceKey(),
                        companionOption.getCompanionKey()
                ),
                Collections.emptyList(),
                processingTimeMs,
                session.getCreatedAt()
        );
    }

    public List<VibeHistoryResponse> getHistory(Long userId) {
        return vibeSessionRepository.findByUserIdWithDetails(userId).stream()
                .filter(s -> s.getVibeResult() != null)
                .map(this::toHistoryResponse)
                .toList();
    }

    public VibeResultResponse getVibeDetail(Long userId, Long sessionId) {
        VibeSession session = vibeSessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIBE_SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (session.getVibeResult() == null) {
            throw new BusinessException(ErrorCode.VIBE_RESULT_NOT_FOUND);
        }

        return toResultResponse(session);
    }

    private VibeResultResponse toResultResponse(VibeSession session) {
        VibePrompt prompt = session.getVibePrompt();
        VibeResult result = session.getVibeResult();
        List<String> moodValues = resolveMoodValues(prompt.getMoodKeywordIds());
        List<CategoryRecommendation> recommendations = getVibeItemsInternal(result.getResultId());

        return new VibeResultResponse(
                session.getSessionId(),
                result.getResultId(),
                result.getPhrase(),
                result.getAiAnalysis(),
                result.getGeneratedImageUrl() != null ? s3StorageService.toPresignedUrl(result.getGeneratedImageUrl()) : null,
                new SelectedOptions(
                        moodValues,
                        prompt.getTimeOption() != null ? prompt.getTimeOption().getTimeKey() : null,
                        prompt.getWeatherOption() != null ? prompt.getWeatherOption().getWeatherKey() : null,
                        prompt.getPlaceOption() != null ? prompt.getPlaceOption().getPlaceKey() : null,
                        prompt.getCompanionOption() != null ? prompt.getCompanionOption().getCompanionKey() : null
                ),
                recommendations,
                result.getProcessingTimeMs(),
                session.getCreatedAt()
        );
    }

    private VibeHistoryResponse toHistoryResponse(VibeSession session) {
        VibePrompt prompt = session.getVibePrompt();
        VibeResult result = session.getVibeResult();
        List<String> moodValues = resolveMoodValues(prompt != null ? prompt.getMoodKeywordIds() : null);

        return new VibeHistoryResponse(
                session.getSessionId(),
                result.getResultId(),
                result.getPhrase(),
                result.getGeneratedImageUrl() != null ? s3StorageService.toPresignedUrl(result.getGeneratedImageUrl()) : null,
                moodValues,
                prompt != null && prompt.getTimeOption() != null ? prompt.getTimeOption().getTimeKey() : null,
                prompt != null && prompt.getWeatherOption() != null ? prompt.getWeatherOption().getWeatherKey() : null,
                prompt != null && prompt.getPlaceOption() != null ? prompt.getPlaceOption().getPlaceKey() : null,
                prompt != null && prompt.getCompanionOption() != null ? prompt.getCompanionOption().getCompanionKey() : null,
                session.getCreatedAt()
        );
    }

    @Transactional
    public VibeItemLikeResponse toggleLike(Long vibeItemId) {
        VibeItem vibeItem = vibeItemRepository.findById(vibeItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIBE_ITEM_NOT_FOUND));
        vibeItem.toggleLike();
        return new VibeItemLikeResponse(vibeItem.getVibeItemId(), vibeItem.getIsUserLiked());
    }

    public List<CategoryRecommendation> getVibeItems(Long userId, Long resultId) {
        VibeResult vibeResult = vibeResultRepository.findById(resultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIBE_RESULT_NOT_FOUND));

        if (!vibeResult.getVibeSession().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return getVibeItemsInternal(resultId);
    }

    private List<CategoryRecommendation> getVibeItemsInternal(Long resultId) {
        List<VibeItem> vibeItems = vibeItemRepository.findByResultIdWithItemDetails(resultId);
        Long languageId = LanguageContext.getLanguageId();

        Map<String, List<RecommendedItemResponse>> grouped = vibeItems.stream()
                .collect(Collectors.groupingBy(
                        vi -> vi.getItem().getCategory().getCategoryKey(),
                        LinkedHashMap::new,
                        Collectors.mapping(vi -> {
                            String itemName = itemTranslationRepository
                                    .findByItemItemIdAndLanguageLanguageId(vi.getItem().getItemId(), languageId)
                                    .map(t -> t.getItemValue())
                                    .orElse(vi.getItem().getItemKey());

                            return new RecommendedItemResponse(
                                    vi.getItem().getItemId(),
                                    vi.getItem().getItemKey(),
                                    itemName,
                                    vi.getItem().getCategory().getCategoryKey(),
                                    vi.getItem().getBrand(),
                                    vi.getItem().getImageUrl(),
                                    vi.getItem().getExternalLink(),
                                    vi.getItem().getExternalService(),
                                    vi.getMatchScore(),
                                    vi.getRecommendReason()
                            );
                        }, Collectors.toList())
                ));

        return grouped.entrySet().stream()
                .map(e -> new CategoryRecommendation(e.getKey(), e.getValue()))
                .toList();
    }

    private List<String> resolveMoodValues(String moodKeywordIdsJson) {
        if (moodKeywordIdsJson == null) return Collections.emptyList();
        List<Long> ids = fromJson(moodKeywordIdsJson);
        return moodKeywordRepository.findAllById(ids).stream()
                .map(MoodKeyword::getKeywordValue)
                .toList();
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "JSON 변환 실패");
        }
    }

    private List<Long> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
