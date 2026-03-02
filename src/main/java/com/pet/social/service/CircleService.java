package com.pet.social.service;

import com.pet.social.domain.CircleEntry;
import com.pet.social.domain.CircleInviteEntry;
import com.pet.social.domain.CircleMemberEntry;
import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserRole;
import com.pet.social.store.AppDataStore;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class CircleService {
    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final AppDataStore dataStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public CircleService(AppDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<CircleEntry> listMyCircles(UserAccount actor) {
        return dataStore.listCirclesByUser(actor.getId());
    }

    public CircleEntry createCircle(UserAccount actor, String name, String description) {
        return dataStore.createCircle(actor.getId(), name, description);
    }

    public CircleEntry updateCircle(UserAccount actor, long circleId, String name, String description) {
        return dataStore.updateCircle(actor.getId(), circleId, name, description);
    }

    public void dissolveCircle(UserAccount actor, long circleId) {
        dataStore.dissolveCircle(actor.getId(), circleId);
    }

    public List<CircleMemberEntry> listMembers(UserAccount actor, long circleId) {
        requireCircleMember(actor, circleId);
        return dataStore.listCircleMembers(circleId);
    }

    public CircleInviteEntry createInvite(UserAccount actor, long circleId) {
        requireActiveCircle(circleId);
        if (!dataStore.isCircleOwner(circleId, actor.getId())) {
            throw new SecurityException("只有圈主可以发起邀请");
        }
        String token = generateToken();
        return dataStore.createCircleInvite(circleId, actor.getId(), token, Instant.now().plus(INVITE_TTL));
    }

    public CircleEntry joinByToken(UserAccount actor, String token) {
        CircleInviteEntry invite = dataStore.findCircleInviteByToken(token);
        if (invite == null || !invite.isActive()) {
            throw new IllegalArgumentException("邀请无效");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("邀请已过期");
        }
        CircleEntry circle = requireActiveCircle(invite.getCircleId());
        dataStore.addCircleMember(circle.getId(), actor.getId());
        return circle;
    }

    public void removeMember(UserAccount actor, long circleId, long memberUserId) {
        CircleEntry circle = requireActiveCircle(circleId);
        if (!dataStore.isCircleOwner(circleId, actor.getId())) {
            throw new SecurityException("只有圈主可以移除成员");
        }
        if (circle.getOwnerId() == memberUserId) {
            throw new IllegalArgumentException("圈主不能移除自己");
        }
        if (!dataStore.removeCircleMember(circleId, memberUserId)) {
            throw new IllegalArgumentException("成员不存在");
        }
    }

    public void leaveCircle(UserAccount actor, long circleId) {
        CircleEntry circle = requireActiveCircle(circleId);
        if (circle.getOwnerId() == actor.getId()) {
            throw new IllegalArgumentException("圈主不能直接退出，请先解散圈子");
        }
        if (!dataStore.removeCircleMember(circleId, actor.getId())) {
            throw new IllegalArgumentException("你不在该圈子中");
        }
    }

    public CircleEntry getAccessibleCircle(UserAccount actor, long circleId) {
        requireCircleMember(actor, circleId);
        return requireActiveCircle(circleId);
    }

    public long countMembers(long circleId) {
        return dataStore.countCircleMembers(circleId);
    }

    private CircleEntry requireActiveCircle(long circleId) {
        CircleEntry circle = dataStore.getCircle(circleId);
        if (circle == null) {
            throw new IllegalArgumentException("圈子不存在");
        }
        if (circle.isDissolved()) {
            throw new IllegalArgumentException("圈子已解散");
        }
        return circle;
    }

    private void requireCircleMember(UserAccount actor, long circleId) {
        CircleEntry circle = requireActiveCircle(circleId);
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!dataStore.isCircleMember(circleId, actor.getId())) {
            throw new SecurityException("仅圈子成员可访问该圈子");
        }
    }

    private String generateToken() {
        byte[] raw = new byte[18];
        secureRandom.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
