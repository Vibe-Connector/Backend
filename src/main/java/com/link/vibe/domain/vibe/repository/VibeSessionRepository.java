package com.link.vibe.domain.vibe.repository;

import com.link.vibe.domain.vibe.entity.VibeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VibeSessionRepository extends JpaRepository<VibeSession, Long> {

    @Query("SELECT vs FROM VibeSession vs " +
            "LEFT JOIN FETCH vs.vibePrompt vp " +
            "LEFT JOIN FETCH vp.timeOption " +
            "LEFT JOIN FETCH vp.weatherOption " +
            "LEFT JOIN FETCH vp.placeOption " +
            "LEFT JOIN FETCH vp.companionOption " +
            "LEFT JOIN FETCH vs.vibeResult " +
            "WHERE vs.userId = :userId " +
            "ORDER BY vs.createdAt DESC")
    List<VibeSession> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT vs FROM VibeSession vs " +
            "LEFT JOIN FETCH vs.vibePrompt vp " +
            "LEFT JOIN FETCH vp.timeOption " +
            "LEFT JOIN FETCH vp.weatherOption " +
            "LEFT JOIN FETCH vp.placeOption " +
            "LEFT JOIN FETCH vp.companionOption " +
            "LEFT JOIN FETCH vs.vibeResult " +
            "WHERE vs.sessionId = :sessionId")
    Optional<VibeSession> findByIdWithDetails(@Param("sessionId") Long sessionId);
}
