package com.pet.social.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostEntry {
    private final long id;
    private final long authorId;
    private final Long petId;
    private final String content;
    private final List<String> imageUrls;
    private final String videoUrl;
    private final String topic;
    private final PostVisibility visibility;
    private final List<Long> visibleCircleIds;
    private ReviewStatus reviewStatus;
    private String reviewReason;
    private int likeCount;
    private int commentCount;
    private final Instant createdAt;
    private Instant updatedAt;

    public PostEntry(long id, long authorId, Long petId, String content, List<String> imageUrls, String videoUrl, String topic,
                     PostVisibility visibility, List<Long> visibleCircleIds, ReviewStatus reviewStatus, String reviewReason,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.petId = petId;
        this.content = content;
        this.imageUrls = new ArrayList<>(imageUrls);
        this.videoUrl = videoUrl;
        this.topic = topic;
        this.visibility = visibility;
        this.visibleCircleIds = new ArrayList<>(visibleCircleIds);
        this.reviewStatus = reviewStatus;
        this.reviewReason = reviewReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getAuthorId() {
        return authorId;
    }

    public Long getPetId() {
        return petId;
    }

    public String getContent() {
        return content;
    }

    public List<String> getImageUrls() {
        return Collections.unmodifiableList(imageUrls);
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getTopic() {
        return topic;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public List<Long> getVisibleCircleIds() {
        return Collections.unmodifiableList(visibleCircleIds);
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
        this.updatedAt = Instant.now();
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
        this.updatedAt = Instant.now();
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
        this.updatedAt = Instant.now();
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
