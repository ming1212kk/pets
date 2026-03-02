package com.pet.social.service;

import com.pet.social.domain.CommentEntry;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.ReviewStatus;
import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserRole;
import com.pet.social.domain.UserStatus;
import com.pet.social.store.InMemoryDataStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class PostService {
    private final InMemoryDataStore dataStore;
    private final ContentSafetyService contentSafetyService;

    public PostService(InMemoryDataStore dataStore, ContentSafetyService contentSafetyService) {
        this.dataStore = dataStore;
        this.contentSafetyService = contentSafetyService;
    }

    public PostEntry createPost(UserAccount actor, Long petId, String content, List<String> imageUrls, String videoUrl, String topic) {
        if (actor.getStatus() == UserStatus.BANNED) {
            throw new IllegalArgumentException("当前账号已被封禁");
        }
        List<String> safeImages = imageUrls == null ? Collections.emptyList() : imageUrls;
        if (petId != null) {
            PetProfile pet = dataStore.findPet(petId).orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
            if (pet.getOwnerId() != actor.getId()) {
                throw new IllegalArgumentException("只能使用自己的宠物档案发帖");
            }
        }

        contentSafetyService.reviewTextAndImages(content, safeImages).ifPresent(reason -> {
            throw new IllegalArgumentException("内容审核未通过: " + reason);
        });

        boolean hasVideo = videoUrl != null && !videoUrl.isBlank();
        PostEntry post = dataStore.createPost(
            actor.getId(),
            petId,
            content,
            safeImages,
            hasVideo ? videoUrl : null,
            topic,
            hasVideo ? ReviewStatus.PENDING_REVIEW : ReviewStatus.APPROVED,
            hasVideo ? "视频审核中" : "审核通过"
        );
        if (hasVideo) {
            contentSafetyService.scheduleVideoReview(post.getId(), content, videoUrl);
        }
        return post;
    }

    public List<PostEntry> listPublicPosts() {
        return dataStore.listApprovedPublicPosts();
    }

    public PostEntry requireVisiblePost(long postId) {
        PostEntry post = dataStore.getPost(postId);
        if (post == null || post.getReviewStatus() != ReviewStatus.APPROVED) {
            throw new IllegalArgumentException("动态不存在或暂不可见");
        }
        return post;
    }

    public void addLike(long postId, UserAccount actor) {
        PostEntry post = requireVisiblePost(postId);
        if (post.getAuthorId() == actor.getId() && actor.getRole() == UserRole.USER) {
            dataStore.addLike(post.getId(), actor.getId());
            return;
        }
        dataStore.addLike(post.getId(), actor.getId());
    }

    public void removeLike(long postId, UserAccount actor) {
        PostEntry post = requireVisiblePost(postId);
        dataStore.removeLike(post.getId(), actor.getId());
    }

    public CommentEntry addComment(long postId, UserAccount actor, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        PostEntry post = requireVisiblePost(postId);
        return dataStore.createComment(post.getId(), actor.getId(), content);
    }

    public void deleteComment(long commentId, UserAccount actor) {
        CommentEntry comment = dataStore.getComment(commentId);
        if (comment == null || comment.isDeleted()) {
            throw new IllegalArgumentException("评论不存在");
        }
        if (comment.getAuthorId() != actor.getId() && actor.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("只能删除自己的评论");
        }
        dataStore.deleteComment(commentId);
    }

    public void deletePostAsAdmin(long postId) {
        dataStore.deletePost(postId);
    }
}
