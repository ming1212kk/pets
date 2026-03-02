package com.pet.social.service;

import com.pet.social.domain.CommentEntry;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.ReportEntry;
import com.pet.social.domain.UserAccount;
import com.pet.social.store.AppDataStore;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private static final String TARGET_POST = "POST";
    private static final String TARGET_COMMENT = "COMMENT";

    private final AppDataStore dataStore;
    private final PostService postService;

    public ReportService(AppDataStore dataStore, PostService postService) {
        this.dataStore = dataStore;
        this.postService = postService;
    }

    public ReportEntry createReport(UserAccount actor, String targetType, long targetId, String reason) {
        String normalizedType = normalizeTargetType(targetType);
        if (TARGET_POST.equals(normalizedType)) {
            PostEntry post = postService.requireVisiblePost(targetId, actor);
            return dataStore.createReport(actor.getId(), normalizedType, post.getId(), reason, "PENDING");
        }
        CommentEntry comment = dataStore.getComment(targetId);
        if (comment == null || comment.isDeleted()) {
            throw new IllegalArgumentException("评论不存在");
        }
        postService.requireVisiblePost(comment.getPostId(), actor);
        return dataStore.createReport(actor.getId(), normalizedType, comment.getId(), reason, "PENDING");
    }

    private String normalizeTargetType(String targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("举报目标类型不能为空");
        }
        String normalized = targetType.trim().toUpperCase();
        if (!TARGET_POST.equals(normalized) && !TARGET_COMMENT.equals(normalized)) {
            throw new IllegalArgumentException("举报目标类型仅支持 POST 或 COMMENT");
        }
        return normalized;
    }
}
