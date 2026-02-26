package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.WeatherOptionTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherOptionTranslationRepository extends JpaRepository<WeatherOptionTranslation, Long> {

    List<WeatherOptionTranslation> findByLanguageId(Long languageId);
}
