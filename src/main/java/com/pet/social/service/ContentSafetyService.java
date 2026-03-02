package com.pet.social.service;

import com.pet.social.domain.ReviewStatus;
import com.pet.social.store.AppDataStore;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ContentSafetyService {
    private static final Set<String> BLOCKED_KEYWORDS = Set.of("spam", "赌博", "色情", "violence", "违规");

    private final AppDataStore dataStore;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ContentSafetyService(AppDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Optional<String> reviewTextAndImages(String content, List<String> imageUrls) {
        if (containsBlockedKeyword(content)) {
            return Optional.of("文本命中敏感词");
        }
        boolean imageBlocked = imageUrls.stream().anyMatch(this::containsBlockedKeyword);
        if (imageBlocked) {
            return Optional.of("图片地址命中敏感词");
        }
        return Optional.empty();
    }

    public void scheduleVideoReview(long postId, String content, String videoUrl) {
        scheduler.schedule(() -> {
            String reason = containsBlockedKeyword(content) || containsBlockedKeyword(videoUrl) ? "视频审核未通过" : null;
            dataStore.updatePostReview(
                postId,
                reason == null ? ReviewStatus.APPROVED : ReviewStatus.REJECTED,
                reason == null ? "视频审核通过" : reason
            );
        }, 2, TimeUnit.SECONDS);
    }

    private boolean containsBlockedKeyword(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase();
        return BLOCKED_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
