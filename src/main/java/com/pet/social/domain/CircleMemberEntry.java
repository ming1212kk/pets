package com.pet.social.domain;

import java.time.Instant;

public class CircleMemberEntry {
    private final long id;
    private final long circleId;
    private final long userId;
    private final Instant joinedAt;

    public CircleMemberEntry(long id, long circleId, long userId, Instant joinedAt) {
        this.id = id;
        this.circleId = circleId;
        this.userId = userId;
        this.joinedAt = joinedAt;
    }

    public long getId() {
        return id;
    }

    public long getCircleId() {
        return circleId;
    }

    public long getUserId() {
        return userId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
