package com.link.vibe.domain.option.service;

import com.link.vibe.domain.option.dto.OptionResponse;
import com.link.vibe.domain.option.dto.OptionResponse.*;
import com.link.vibe.domain.option.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionService {

    private final MoodKeywordRepository moodKeywordRepository;
    private final TimeOptionRepository timeOptionRepository;
    private final WeatherOptionRepository weatherOptionRepository;
    private final PlaceOptionRepository placeOptionRepository;
    private final CompanionOptionRepository companionOptionRepository;

    public OptionResponse getAllOptions() {
        var moods = moodKeywordRepository.findByIsActiveTrueOrderBySortOrder().stream()
                .map(m -> new MoodDto(m.getKeywordId(), m.getKeywordKey(), m.getKeywordText(), m.getCategory()))
                .toList();

        var times = timeOptionRepository.findByIsActiveTrueOrderBySortOrder().stream()
                .map(t -> new TimeDto(t.getTimeId(), t.getTimeKey(), t.getTimeText()))
                .toList();

        var weathers = weatherOptionRepository.findByIsActiveTrueOrderBySortOrder().stream()
                .map(w -> new WeatherDto(w.getWeatherId(), w.getWeatherKey(), w.getWeatherText(), w.getIcon()))
                .toList();

        var places = placeOptionRepository.findByIsActiveTrueOrderBySortOrder().stream()
                .map(p -> new PlaceDto(p.getPlaceId(), p.getPlaceKey(), p.getPlaceText(), p.getIcon()))
                .toList();

        var companions = companionOptionRepository.findByIsActiveTrueOrderBySortOrder().stream()
                .map(c -> new CompanionDto(c.getCompanionId(), c.getCompanionKey(), c.getCompanionText(), c.getIcon()))
                .toList();

        return new OptionResponse(moods, times, weathers, places, companions);
    }
}
