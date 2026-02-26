package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.PlaceOptionTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceOptionTranslationRepository extends JpaRepository<PlaceOptionTranslation, Long> {

    List<PlaceOptionTranslation> findByLanguageId(Long languageId);
}
