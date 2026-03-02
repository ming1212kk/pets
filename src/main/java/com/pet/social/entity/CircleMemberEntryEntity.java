package com.pet.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "circle_member_entries", indexes = {
    @Index(name = "idx_circle_member_circle_id", columnList = "circle_id"),
    @Index(name = "idx_circle_member_user_id", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_circle_member_circle_user", columnNames = {"circle_id", "user_id"})
})
public class CircleMemberEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "circle_id", nullable = false)
    private Long circleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public Long getId() {
        return id;
    }

    public Long getCircleId() {
        return circleId;
    }

    public void setCircleId(Long circleId) {
        this.circleId = circleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    @PrePersist
    public void prePersist() {
        joinedAt = Instant.now();
    }
}
