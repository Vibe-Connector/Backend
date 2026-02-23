package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.MoodKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoodKeywordRepository extends JpaRepository<MoodKeyword, Long> {
    List<MoodKeyword> findByIsActiveTrueOrderBySortOrder();
}
