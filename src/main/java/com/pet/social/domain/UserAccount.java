package com.pet.social.domain;

import java.time.Instant;

public class UserAccount {
    private final long id;
    private final String nickname;
    private final String phone;
    private final String wechatKey;
    private final String avatarUrl;
    private final UserRole role;
    private UserStatus status;
    private final Instant createdAt;

    public UserAccount(long id, String nickname, String phone, String wechatKey, String avatarUrl,
                       UserRole role, UserStatus status, Instant createdAt) {
        this.id = id;
        this.nickname = nickname;
        this.phone = phone;
        this.wechatKey = wechatKey;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    public String getWechatKey() {
        return wechatKey;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
