package com.link.vibe.domain.vibe.service;

import com.link.vibe.domain.option.entity.*;
import com.link.vibe.domain.option.repository.*;
import com.link.vibe.domain.vibe.dto.*;
import com.link.vibe.domain.vibe.dto.VibeResultResponse.SelectedOptions;
import com.link.vibe.domain.vibe.entity.VibeRequest;
import com.link.vibe.domain.vibe.entity.VibeRequestMood;
import com.link.vibe.domain.vibe.repository.VibeRequestRepository;
import com.link.vibe.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VibeService {

    private final VibeRequestRepository vibeRequestRepository;
    private final MoodKeywordRepository moodKeywordRepository;
    private final TimeOptionRepository timeOptionRepository;
    private final WeatherOptionRepository weatherOptionRepository;
    private final PlaceOptionRepository placeOptionRepository;
    private final CompanionOptionRepository companionOptionRepository;
    private final OpenAiService openAiService;

    @Transactional
    public VibeResultResponse createVibe(VibeCreateRequest request) {
        List<MoodKeyword> moodKeywords = moodKeywordRepository.findAllById(request.moodKeywordIds());
        if (moodKeywords.size() != request.moodKeywordIds().size()) {
            throw new BusinessException("유효하지 않은 기분 키워드 ID가 포함되어 있습니다.", HttpStatus.BAD_REQUEST);
        }

        TimeOption timeOption = timeOptionRepository.findById(request.timeId())
                .orElseThrow(() -> new BusinessException("유효하지 않은 시간 옵션입니다.", HttpStatus.BAD_REQUEST));
        WeatherOption weatherOption = weatherOptionRepository.findById(request.weatherId())
                .orElseThrow(() -> new BusinessException("유효하지 않은 날씨 옵션입니다.", HttpStatus.BAD_REQUEST));
        PlaceOption placeOption = placeOptionRepository.findById(request.placeId())
                .orElseThrow(() -> new BusinessException("유효하지 않은 공간 옵션입니다.", HttpStatus.BAD_REQUEST));
        CompanionOption companionOption = companionOptionRepository.findById(request.companionId())
                .orElseThrow(() -> new BusinessException("유효하지 않은 동반자 옵션입니다.", HttpStatus.BAD_REQUEST));

        VibeRequest vibeRequest = VibeRequest.builder()
                .sessionId(request.sessionId())
                .timeOption(timeOption)
                .weatherOption(weatherOption)
                .placeOption(placeOption)
                .companionOption(companionOption)
                .build();

        for (MoodKeyword keyword : moodKeywords) {
            vibeRequest.addMood(new VibeRequestMood(keyword));
        }

        List<String> moodTexts = moodKeywords.stream().map(MoodKeyword::getKeywordText).toList();

        long startTime = System.currentTimeMillis();
        OpenAiService.VibeResult aiResult = openAiService.generateVibe(
                moodTexts,
                timeOption.getTimeText(),
                weatherOption.getWeatherText(),
                placeOption.getPlaceText(),
                companionOption.getCompanionText()
        );
        int processingTimeMs = (int) (System.currentTimeMillis() - startTime);

        String finalPrompt = openAiService.buildUserPrompt(
                moodTexts,
                timeOption.getTimeText(),
                weatherOption.getWeatherText(),
                placeOption.getPlaceText(),
                companionOption.getCompanionText()
        );

        vibeRequest.applyResult(finalPrompt, aiResult.phrase(), aiResult.analysis(), processingTimeMs);
        vibeRequestRepository.save(vibeRequest);

        return new VibeResultResponse(
                vibeRequest.getVibeId(),
                aiResult.phrase(),
                aiResult.analysis(),
                new SelectedOptions(
                        moodTexts,
                        timeOption.getTimeText(),
                        weatherOption.getWeatherText(),
                        placeOption.getPlaceText(),
                        companionOption.getCompanionText()
                ),
                processingTimeMs,
                vibeRequest.getCreatedAt()
        );
    }

    public List<VibeHistoryResponse> getHistory(String sessionId) {
        return vibeRequestRepository.findBySessionIdWithOptions(sessionId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    public VibeResultResponse getVibeDetail(Long vibeId) {
        VibeRequest vr = vibeRequestRepository.findByIdWithOptions(vibeId)
                .orElseThrow(() -> new BusinessException("해당 Vibe를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        List<String> moodTexts = vr.getMoods().stream()
                .map(m -> m.getMoodKeyword().getKeywordText())
                .toList();

        return new VibeResultResponse(
                vr.getVibeId(),
                vr.getResultPhrase(),
                vr.getResultAnalysis(),
                new SelectedOptions(
                        moodTexts,
                        vr.getTimeOption().getTimeText(),
                        vr.getWeatherOption().getWeatherText(),
                        vr.getPlaceOption().getPlaceText(),
                        vr.getCompanionOption().getCompanionText()
                ),
                vr.getProcessingTimeMs(),
                vr.getCreatedAt()
        );
    }

    private VibeHistoryResponse toHistoryResponse(VibeRequest vr) {
        List<String> moodTexts = vr.getMoods().stream()
                .map(m -> m.getMoodKeyword().getKeywordText())
                .toList();

        return new VibeHistoryResponse(
                vr.getVibeId(),
                vr.getResultPhrase(),
                moodTexts,
                vr.getTimeOption().getTimeText(),
                vr.getWeatherOption().getWeatherText(),
                vr.getPlaceOption().getPlaceText(),
                vr.getCompanionOption().getCompanionText(),
                vr.getCreatedAt()
        );
    }
}
