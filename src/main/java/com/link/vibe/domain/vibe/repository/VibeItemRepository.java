package com.link.vibe.domain.vibe.repository;

import com.link.vibe.domain.vibe.entity.VibeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VibeItemRepository extends JpaRepository<VibeItem, Long> {

    List<VibeItem> findByVibeResultResultId(Long resultId);
}
