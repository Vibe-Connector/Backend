package com.link.vibe.domain.item.repository;

import com.link.vibe.domain.item.entity.MovieDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieDetailRepository extends JpaRepository<MovieDetail, Long> {

    Optional<MovieDetail> findByItemId(Long itemId);
}
