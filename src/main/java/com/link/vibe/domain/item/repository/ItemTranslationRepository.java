package com.link.vibe.domain.item.repository;

import com.link.vibe.domain.item.entity.ItemTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemTranslationRepository extends JpaRepository<ItemTranslation, Long> {

    Optional<ItemTranslation> findByItemItemIdAndLanguageLanguageId(
            Long itemId, Long languageId);
}
