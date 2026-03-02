package com.pet.social.entity;

import com.pet.social.domain.PostVisibility;
import com.pet.social.domain.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "post_entries", indexes = {
    @Index(name = "idx_post_author_id", columnList = "author_id"),
    @Index(name = "idx_post_review_status", columnList = "review_status"),
    @Index(name = "idx_post_created_at", columnList = "created_at")
})
public class PostEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "pet_id")
    private Long petId;

    @Lob
    @Column(nullable = false)
    private String content;

    @Lob
    @Column(name = "image_urls_json", nullable = false)
    private String imageUrlsJson;

    @Column(name = "video_url", length = 512)
    private String videoUrl;

    @Column(length = 64)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PostVisibility visibility;

    @Lob
    @Column(name = "visible_circle_ids_json")
    private String visibleCircleIdsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    private ReviewStatus reviewStatus;

    @Column(name = "review_reason", length = 255)
    private String reviewReason;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrlsJson() {
        return imageUrlsJson;
    }

    public void setImageUrlsJson(String imageUrlsJson) {
        this.imageUrlsJson = imageUrlsJson;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PostVisibility visibility) {
        this.visibility = visibility;
    }

    public String getVisibleCircleIdsJson() {
        return visibleCircleIdsJson;
    }

    public void setVisibleCircleIdsJson(String visibleCircleIdsJson) {
        this.visibleCircleIdsJson = visibleCircleIdsJson;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
