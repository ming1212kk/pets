package com.pet.social.domain;

import java.time.Instant;

public class CommentEntry {
    private final long id;
    private final long postId;
    private final long authorId;
    private final String content;
    private boolean deleted;
    private final Instant createdAt;
    private Instant updatedAt;

    public CommentEntry(long id, long postId, long authorId, String content, boolean deleted, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getPostId() {
        return postId;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void markDeleted() {
        this.deleted = true;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
