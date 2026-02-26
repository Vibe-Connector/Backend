package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.CompanionOptionTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanionOptionTranslationRepository extends JpaRepository<CompanionOptionTranslation, Long> {

    List<CompanionOptionTranslation> findByLanguageId(Long languageId);
}
