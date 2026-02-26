package com.link.vibe.domain.follow.repository;

import com.link.vibe.domain.follow.entity.Follow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerUserIdAndFollowingUserId(Long followerId, Long followingId);

    boolean existsByFollowerUserIdAndFollowingUserId(Long followerId, Long followingId);

    long countByFollowingUserId(Long followingId);

    long countByFollowerUserId(Long followerId);

    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.userId = :followingId AND f.followId < :cursor ORDER BY f.followId DESC")
    List<Follow> findFollowersByCursor(@Param("followingId") Long followingId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.userId = :followingId ORDER BY f.followId DESC")
    List<Follow> findFollowers(@Param("followingId") Long followingId, Pageable pageable);

    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.userId = :followerId AND f.followId < :cursor ORDER BY f.followId DESC")
    List<Follow> findFollowingsByCursor(@Param("followerId") Long followerId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.userId = :followerId ORDER BY f.followId DESC")
    List<Follow> findFollowings(@Param("followerId") Long followerId, Pageable pageable);
}
