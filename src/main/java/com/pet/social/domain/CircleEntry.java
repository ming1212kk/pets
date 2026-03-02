package com.pet.social.domain;

import java.time.Instant;

public class CircleEntry {
    private final long id;
    private final long ownerId;
    private final String name;
    private final String description;
    private final boolean dissolved;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CircleEntry(long id, long ownerId, String name, String description, boolean dissolved, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.dissolved = dissolved;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDissolved() {
        return dissolved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
