package com.link.vibe.domain.item.repository;

import com.link.vibe.domain.item.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {

    Optional<ItemCategory> findByCategoryKey(String categoryKey);
}
