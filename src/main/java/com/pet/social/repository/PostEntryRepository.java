package com.pet.social.repository;

import com.pet.social.domain.ReviewStatus;
import com.pet.social.entity.PostEntryEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostEntryRepository extends JpaRepository<PostEntryEntity, Long> {
    List<PostEntryEntity> findByReviewStatusOrderByCreatedAtDesc(ReviewStatus reviewStatus);

    default List<PostEntryEntity> findAllOrderByCreatedAtDesc() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
