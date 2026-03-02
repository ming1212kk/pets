package com.pet.social.domain;

import java.time.Instant;

public class CircleInviteEntry {
    private final long id;
    private final long circleId;
    private final long createdBy;
    private final String token;
    private final boolean active;
    private final Instant expiresAt;
    private final Instant createdAt;

    public CircleInviteEntry(long id, long circleId, long createdBy, String token, boolean active, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.circleId = circleId;
        this.createdBy = createdBy;
        this.token = token;
        this.active = active;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getCircleId() {
        return circleId;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public String getToken() {
        return token;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
