package com.link.vibe.domain.vibe.repository;

import com.link.vibe.domain.vibe.entity.VibeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VibeRequestRepository extends JpaRepository<VibeRequest, Long> {

    @Query("SELECT vr FROM VibeRequest vr " +
            "JOIN FETCH vr.timeOption " +
            "JOIN FETCH vr.weatherOption " +
            "JOIN FETCH vr.placeOption " +
            "JOIN FETCH vr.companionOption " +
            "JOIN FETCH vr.moods m " +
            "JOIN FETCH m.moodKeyword " +
            "WHERE vr.sessionId = :sessionId " +
            "ORDER BY vr.createdAt DESC")
    List<VibeRequest> findBySessionIdWithOptions(@Param("sessionId") String sessionId);

    @Query("SELECT vr FROM VibeRequest vr " +
            "JOIN FETCH vr.timeOption " +
            "JOIN FETCH vr.weatherOption " +
            "JOIN FETCH vr.placeOption " +
            "JOIN FETCH vr.companionOption " +
            "JOIN FETCH vr.moods m " +
            "JOIN FETCH m.moodKeyword " +
            "WHERE vr.vibeId = :vibeId")
    Optional<VibeRequest> findByIdWithOptions(@Param("vibeId") Long vibeId);
}
