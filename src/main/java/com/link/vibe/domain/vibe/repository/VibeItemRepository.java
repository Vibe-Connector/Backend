package com.link.vibe.domain.vibe.repository;

import com.link.vibe.domain.vibe.entity.VibeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VibeItemRepository extends JpaRepository<VibeItem, Long> {

    List<VibeItem> findByVibeResultResultId(Long resultId);

    @Query("SELECT vi FROM VibeItem vi " +
            "JOIN FETCH vi.item i " +
            "JOIN FETCH i.category " +
            "WHERE vi.vibeResult.resultId = :resultId " +
            "ORDER BY vi.matchScore DESC")
    List<VibeItem> findByResultIdWithItemDetails(@Param("resultId") Long resultId);
}
