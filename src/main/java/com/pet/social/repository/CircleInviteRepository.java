package com.pet.social.repository;

import com.pet.social.entity.CircleInviteEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CircleInviteRepository extends JpaRepository<CircleInviteEntryEntity, Long> {
    Optional<CircleInviteEntryEntity> findByToken(String token);

    List<CircleInviteEntryEntity> findByCircleIdAndActiveTrue(Long circleId);
}
