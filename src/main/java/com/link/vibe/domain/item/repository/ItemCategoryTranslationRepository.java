package com.link.vibe.domain.item.repository;

import com.link.vibe.domain.item.entity.ItemCategoryTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemCategoryTranslationRepository extends JpaRepository<ItemCategoryTranslation, Long> {

    Optional<ItemCategoryTranslation> findByCategoryCategoryIdAndLanguageLanguageId(
            Long categoryId, Long languageId);
}
