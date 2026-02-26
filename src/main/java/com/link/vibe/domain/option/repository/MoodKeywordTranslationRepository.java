package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.MoodKeywordTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoodKeywordTranslationRepository extends JpaRepository<MoodKeywordTranslation, Long> {

    List<MoodKeywordTranslation> findByLanguageId(Long languageId);
}
