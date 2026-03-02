package com.pet.social.repository;

import com.pet.social.entity.CircleMemberEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CircleMemberRepository extends JpaRepository<CircleMemberEntryEntity, Long> {
    boolean existsByCircleIdAndUserId(Long circleId, Long userId);

    Optional<CircleMemberEntryEntity> findByCircleIdAndUserId(Long circleId, Long userId);

    List<CircleMemberEntryEntity> findByCircleIdOrderByJoinedAtAsc(Long circleId);

    List<CircleMemberEntryEntity> findByUserIdOrderByJoinedAtDesc(Long userId);

    long countByCircleId(Long circleId);
}
