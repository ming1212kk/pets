package com.pet.social.repository;

import com.pet.social.entity.CommentEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentEntryRepository extends JpaRepository<CommentEntryEntity, Long> {
    List<CommentEntryEntity> findByPostIdAndDeletedFalseOrderByCreatedAtAsc(Long postId);

    long countByPostIdAndDeletedFalse(Long postId);
}
