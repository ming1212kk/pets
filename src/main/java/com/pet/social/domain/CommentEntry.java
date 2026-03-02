package com.pet.social.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommentEntry {
    private final long id;
    private final long postId;
    private final long authorId;
    private final Long parentCommentId;
    private final Long replyToUserId;
    private final List<Long> mentionUserIds;
    private final String content;
    private boolean deleted;
    private final Instant createdAt;
    private Instant updatedAt;

    public CommentEntry(long id, long postId, long authorId, Long parentCommentId, Long replyToUserId, List<Long> mentionUserIds,
                        String content, boolean deleted, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.replyToUserId = replyToUserId;
        this.mentionUserIds = new ArrayList<>(mentionUserIds);
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

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public Long getReplyToUserId() {
        return replyToUserId;
    }

    public List<Long> getMentionUserIds() {
        return Collections.unmodifiableList(mentionUserIds);
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
