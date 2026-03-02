package com.pet.social.domain;

import java.time.Instant;

public class ReportEntry {
    private final long id;
    private final long reporterId;
    private final String targetType;
    private final long targetId;
    private final String reason;
    private final String status;
    private final Instant createdAt;

    public ReportEntry(long id, long reporterId, String targetType, long targetId, String reason, String status, Instant createdAt) {
        this.id = id;
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getReporterId() {
        return reporterId;
    }

    public String getTargetType() {
        return targetType;
    }

    public long getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
