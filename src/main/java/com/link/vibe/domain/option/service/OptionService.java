package com.link.vibe.domain.option.service;

import com.link.vibe.domain.language.entity.Language;
import com.link.vibe.domain.language.repository.LanguageRepository;
import com.link.vibe.domain.option.dto.OptionResponse;
import com.link.vibe.domain.option.dto.OptionResponse.*;
import com.link.vibe.domain.option.repository.*;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionService {

    private final MoodKeywordRepository moodKeywordRepository;
    private final TimeOptionRepository timeOptionRepository;
    private final WeatherOptionRepository weatherOptionRepository;
    private final PlaceOptionRepository placeOptionRepository;
    private final CompanionOptionRepository companionOptionRepository;
    private final LanguageRepository languageRepository;
    private final MoodKeywordTranslationRepository moodTranslationRepository;
    private final WeatherOptionTranslationRepository weatherTranslationRepository;
    private final PlaceOptionTranslationRepository placeTranslationRepository;
    private final CompanionOptionTranslationRepository companionTranslationRepository;

    public OptionResponse getAllOptions(String lang) {
        Long languageId = resolveLanguageId(lang);

        // 번역 맵 구성 (옵션ID → 번역 텍스트)
        Map<Long, String> moodLabels = moodTranslationRepository.findByLanguageId(languageId).stream()
                .collect(Collectors.toMap(t -> t.getKeywordId(), t -> t.getKeywordValue()));

        Map<Long, String> weatherLabels = weatherTranslationRepository.findByLanguageId(languageId).stream()
                .collect(Collectors.toMap(t -> t.getWeatherId(), t -> t.getWeatherValue()));

        Map<Long, String> placeLabels = placeTranslationRepository.findByLanguageId(languageId).stream()
                .collect(Collectors.toMap(t -> t.getPlaceId(), t -> t.getPlaceValue()));

        Map<Long, String> companionLabels = companionTranslationRepository.findByLanguageId(languageId).stream()
                .collect(Collectors.toMap(t -> t.getCompanionId(), t -> t.getCompanionValue()));

        var moods = moodKeywordRepository.findAllByOrderByKeywordId().stream()
                .map(m -> new MoodDto(
                        m.getKeywordId(),
                        m.getKeywordValue(),
                        m.getCategory(),
                        moodLabels.getOrDefault(m.getKeywordId(), m.getKeywordValue())
                ))
                .toList();

        var times = timeOptionRepository.findByIsActiveTrueOrderByTimeId().stream()
                .map(t -> new TimeDto(t.getTimeId(), t.getTimeKey(), t.getTimeValue().toString(), t.getPeriod()))
                .toList();

        var weathers = weatherOptionRepository.findByIsActiveTrueOrderByWeatherId().stream()
                .map(w -> new WeatherDto(
                        w.getWeatherId(),
                        w.getWeatherKey(),
                        weatherLabels.getOrDefault(w.getWeatherId(), w.getWeatherKey())
                ))
                .toList();

        var places = placeOptionRepository.findByIsActiveTrueOrderByPlaceId().stream()
                .map(p -> new PlaceDto(
                        p.getPlaceId(),
                        p.getPlaceKey(),
                        placeLabels.getOrDefault(p.getPlaceId(), p.getPlaceKey())
                ))
                .toList();

        var companions = companionOptionRepository.findByIsActiveTrueOrderByCompanionId().stream()
                .map(c -> new CompanionDto(
                        c.getCompanionId(),
                        c.getCompanionKey(),
                        companionLabels.getOrDefault(c.getCompanionId(), c.getCompanionKey())
                ))
                .toList();

        return new OptionResponse(moods, times, weathers, places, companions);
    }

    private Long resolveLanguageId(String lang) {
        if (lang == null || lang.isBlank()) {
            lang = "ko";
        }
        Language language = languageRepository.findByLanguageCode(lang)
                .orElseThrow(() -> new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED));
        return language.getLanguageId();
    }
}
