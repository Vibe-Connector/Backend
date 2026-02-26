package com.link.vibe.domain.vibe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.domain.option.entity.*;
import com.link.vibe.domain.option.repository.*;
import com.link.vibe.domain.vibe.dto.*;
import com.link.vibe.domain.vibe.dto.VibeResultResponse.SelectedOptions;
import com.link.vibe.domain.vibe.entity.VibePrompt;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibePromptRepository;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VibeService {

    private final VibeSessionRepository vibeSessionRepository;
    private final VibePromptRepository vibePromptRepository;
    private final VibeResultRepository vibeResultRepository;
    private final MoodKeywordRepository moodKeywordRepository;
    private final TimeOptionRepository timeOptionRepository;
    private final WeatherOptionRepository weatherOptionRepository;
    private final PlaceOptionRepository placeOptionRepository;
    private final CompanionOptionRepository companionOptionRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    @Value("${openai.model:gpt-4o-mini}")
    private String aiModel;

    private static final Long DEFAULT_USER_ID = 1L;

    @Transactional
    public VibeResultResponse createVibe(VibeCreateRequest request) {
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
        VibeSession session = VibeSession.builder().userId(DEFAULT_USER_ID).build();
        vibeSessionRepository.save(session);

        // 2. OpenAI 호출
        List<String> moodValues = moodKeywords.stream().map(MoodKeyword::getKeywordValue).toList();

        long startTime = System.currentTimeMillis();
        OpenAiService.VibeResult aiResult = openAiService.generateVibe(
                moodValues,
                timeOption.getTimeKey(),
                weatherOption.getWeatherKey(),
                placeOption.getPlaceKey(),
                companionOption.getCompanionKey()
        );
        int processingTimeMs = (int) (System.currentTimeMillis() - startTime);

        String finalPrompt = openAiService.buildUserPrompt(
                moodValues,
                timeOption.getTimeKey(),
                weatherOption.getWeatherKey(),
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
                aiResult.phrase(),
                aiResult.analysis(),
                new SelectedOptions(
                        moodValues,
                        timeOption.getTimeKey(),
                        weatherOption.getWeatherKey(),
                        placeOption.getPlaceKey(),
                        companionOption.getCompanionKey()
                ),
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

    public VibeResultResponse getVibeDetail(Long sessionId) {
        VibeSession session = vibeSessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIBE_SESSION_NOT_FOUND));

        if (session.getVibeResult() == null) {
            throw new BusinessException(ErrorCode.VIBE_RESULT_NOT_FOUND);
        }

        return toResultResponse(session);
    }

    private VibeResultResponse toResultResponse(VibeSession session) {
        VibePrompt prompt = session.getVibePrompt();
        VibeResult result = session.getVibeResult();
        List<String> moodValues = resolveMoodValues(prompt.getMoodKeywordIds());

        return new VibeResultResponse(
                session.getSessionId(),
                result.getPhrase(),
                result.getAiAnalysis(),
                new SelectedOptions(
                        moodValues,
                        prompt.getTimeOption() != null ? prompt.getTimeOption().getTimeKey() : null,
                        prompt.getWeatherOption() != null ? prompt.getWeatherOption().getWeatherKey() : null,
                        prompt.getPlaceOption() != null ? prompt.getPlaceOption().getPlaceKey() : null,
                        prompt.getCompanionOption() != null ? prompt.getCompanionOption().getCompanionKey() : null
                ),
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
                result.getPhrase(),
                moodValues,
                prompt != null && prompt.getTimeOption() != null ? prompt.getTimeOption().getTimeKey() : null,
                prompt != null && prompt.getWeatherOption() != null ? prompt.getWeatherOption().getWeatherKey() : null,
                prompt != null && prompt.getPlaceOption() != null ? prompt.getPlaceOption().getPlaceKey() : null,
                prompt != null && prompt.getCompanionOption() != null ? prompt.getCompanionOption().getCompanionKey() : null,
                session.getCreatedAt()
        );
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
