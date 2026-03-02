package com.pet.social.repository;

import com.pet.social.entity.PostLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLikeEntity, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    Optional<PostLikeEntity> findByPostIdAndUserId(Long postId, Long userId);
}
