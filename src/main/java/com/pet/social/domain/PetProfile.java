package com.pet.social.domain;

import java.time.Instant;

public class PetProfile {
    private final long id;
    private final long ownerId;
    private String name;
    private String type;
    private Integer ageMonths;
    private boolean defaultPet;
    private final Instant createdAt;
    private Instant updatedAt;

    public PetProfile(long id, long ownerId, String name, String type, Integer ageMonths, boolean defaultPet, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.type = type;
        this.ageMonths = ageMonths;
        this.defaultPet = defaultPet;
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

    public String getType() {
        return type;
    }

    public Integer getAgeMonths() {
        return ageMonths;
    }

    public boolean isDefaultPet() {
        return defaultPet;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, String type, Integer ageMonths) {
        this.name = name;
        this.type = type;
        this.ageMonths = ageMonths;
        this.updatedAt = Instant.now();
    }

    public void setDefaultPet(boolean defaultPet) {
        this.defaultPet = defaultPet;
        this.updatedAt = Instant.now();
    }
}
