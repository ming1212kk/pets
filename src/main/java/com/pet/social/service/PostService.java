package com.pet.social.service;

import com.pet.social.domain.CircleEntry;
import com.pet.social.domain.CommentEntry;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.PostVisibility;
import com.pet.social.domain.ReviewStatus;
import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserRole;
import com.pet.social.domain.UserStatus;
import com.pet.social.store.AppDataStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class PostService {
    private final AppDataStore dataStore;
    private final ContentSafetyService contentSafetyService;

    public PostService(AppDataStore dataStore, ContentSafetyService contentSafetyService) {
        this.dataStore = dataStore;
        this.contentSafetyService = contentSafetyService;
    }

    public PostEntry createPost(UserAccount actor, Long petId, String content, List<String> imageUrls, String videoUrl, String topic,
                                PostVisibility visibility, List<Long> circleIds) {
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

        PostVisibility resolvedVisibility = visibility == null ? PostVisibility.PUBLIC : visibility;
        List<Long> visibleCircleIds = normalizeCircleIds(resolvedVisibility, circleIds);
        for (Long circleId : visibleCircleIds) {
            CircleEntry circle = dataStore.getCircle(circleId);
            if (circle == null) {
                throw new IllegalArgumentException("圈子不存在");
            }
            if (circle.isDissolved()) {
                throw new IllegalArgumentException("圈子已解散");
            }
            if (!dataStore.isCircleMember(circleId, actor.getId())) {
                throw new SecurityException("只能发到自己已加入的圈子");
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
            resolvedVisibility,
            visibleCircleIds,
            hasVideo ? ReviewStatus.PENDING_REVIEW : ReviewStatus.APPROVED,
            hasVideo ? "视频审核中" : "审核通过"
        );
        if (hasVideo) {
            contentSafetyService.scheduleVideoReview(post.getId(), content, videoUrl);
        }
        return post;
    }

    public List<PostEntry> listVisiblePosts(UserAccount actor) {
        return dataStore.listApprovedVisiblePosts(actor.getId());
    }

    public List<PostEntry> listCirclePosts(long circleId, UserAccount actor) {
        requireCircleMember(circleId, actor);
        return dataStore.listApprovedPostsForCircle(circleId, actor.getId());
    }

    public PostEntry requireVisiblePost(long postId, UserAccount actor) {
        PostEntry post = dataStore.getPost(postId);
        if (post == null || post.getReviewStatus() != ReviewStatus.APPROVED) {
            throw new IllegalArgumentException("动态不存在或暂不可见");
        }
        if (actor.getRole() == UserRole.ADMIN) {
            return post;
        }
        if (post.getVisibility() == PostVisibility.PUBLIC) {
            return post;
        }
        boolean visible = post.getVisibleCircleIds().stream().anyMatch(circleId -> dataStore.isCircleMember(circleId, actor.getId()));
        if (!visible) {
            throw new SecurityException("无权查看该圈子动态");
        }
        return post;
    }

    public void addLike(long postId, UserAccount actor) {
        PostEntry post = requireVisiblePost(postId, actor);
        dataStore.addLike(post.getId(), actor.getId());
    }

    public void removeLike(long postId, UserAccount actor) {
        PostEntry post = requireVisiblePost(postId, actor);
        dataStore.removeLike(post.getId(), actor.getId());
    }

    public CommentEntry addComment(long postId, UserAccount actor, String content, Long parentCommentId,
                                   Long replyToUserId, List<Long> mentionUserIds) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        requireVisiblePost(postId, actor);
        if (replyToUserId != null && parentCommentId == null) {
            throw new IllegalArgumentException("回复指定用户时必须关联父评论");
        }
        if (parentCommentId != null) {
            CommentEntry parent = dataStore.getComment(parentCommentId);
            if (parent == null || parent.isDeleted() || parent.getPostId() != postId) {
                throw new IllegalArgumentException("回复的目标评论不存在");
            }
            if (replyToUserId != null && dataStore.getUser(replyToUserId) == null) {
                throw new IllegalArgumentException("被回复的用户不存在");
            }
        }
        List<Long> mentions = mentionUserIds == null ? Collections.emptyList() : new ArrayList<>(new LinkedHashSet<>(mentionUserIds));
        for (Long mentionUserId : mentions) {
            if (mentionUserId == null || dataStore.getUser(mentionUserId) == null) {
                throw new IllegalArgumentException("被 @ 的用户不存在");
            }
        }
        return dataStore.createComment(postId, actor.getId(), parentCommentId, replyToUserId, mentions, content);
    }

    public void deleteComment(long commentId, UserAccount actor) {
        CommentEntry comment = dataStore.getComment(commentId);
        if (comment == null || comment.isDeleted()) {
            throw new IllegalArgumentException("评论不存在");
        }
        if (comment.getAuthorId() != actor.getId() && actor.getRole() != UserRole.ADMIN) {
            throw new SecurityException("只能删除自己的评论");
        }
        dataStore.deleteComment(commentId);
    }

    public void deletePostAsAdmin(long postId) {
        dataStore.deletePost(postId);
    }

    private List<Long> normalizeCircleIds(PostVisibility visibility, List<Long> circleIds) {
        if (visibility == PostVisibility.PUBLIC) {
            return Collections.emptyList();
        }
        if (circleIds == null || circleIds.isEmpty()) {
            throw new IllegalArgumentException("圈子可见动态必须至少选择一个圈子");
        }
        List<Long> resolved = new ArrayList<>(new LinkedHashSet<>(circleIds));
        for (Long circleId : resolved) {
            if (circleId == null) {
                throw new IllegalArgumentException("圈子参数不合法");
            }
        }
        return resolved;
    }

    private void requireCircleMember(long circleId, UserAccount actor) {
        CircleEntry circle = dataStore.getCircle(circleId);
        if (circle == null) {
            throw new IllegalArgumentException("圈子不存在");
        }
        if (circle.isDissolved()) {
            throw new IllegalArgumentException("圈子已解散");
        }
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!dataStore.isCircleMember(circleId, actor.getId())) {
            throw new SecurityException("仅圈子成员可查看圈子动态");
        }
    }
}
