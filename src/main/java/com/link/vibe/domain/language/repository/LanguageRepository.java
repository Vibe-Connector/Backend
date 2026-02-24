package com.link.vibe.domain.language.repository;

import com.link.vibe.domain.language.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Long> {

    Optional<Language> findByLanguageCode(String languageCode);

    Optional<Language> findByLanguageCodeAndIsActiveTrue(String languageCode);
}
