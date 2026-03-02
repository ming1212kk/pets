package com.pet.social.controller;

import com.pet.social.config.AuthInterceptor;
import com.pet.social.domain.CommentEntry;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.PostVisibility;
import com.pet.social.domain.UserAccount;
import com.pet.social.service.PostService;
import com.pet.social.store.AppDataStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PostController {
    private final PostService postService;
    private final AppDataStore dataStore;

    public PostController(PostService postService, AppDataStore dataStore) {
        this.postService = postService;
        this.dataStore = dataStore;
    }

    @PostMapping("/posts")
    public PostDetailResponse createPost(@Valid @RequestBody CreatePostRequest request, HttpServletRequest httpRequest) {
        UserAccount actor = currentUser(httpRequest);
        List<String> images = request.imageUrls() == null ? Collections.emptyList() : request.imageUrls();
        PostEntry post = postService.createPost(
            actor,
            request.petId(),
            request.content(),
            images,
            request.videoUrl(),
            request.topic(),
            parseVisibility(request.visibility()),
            request.circleIds()
        );
        return toDetail(post, actor);
    }

    @GetMapping("/posts")
    public List<PostSummaryResponse> listVisiblePosts(HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        return postService.listVisiblePosts(actor).stream().map(post -> toSummary(post, actor)).toList();
    }

    @GetMapping("/posts/{postId}")
    public PostDetailResponse getPost(@PathVariable long postId, HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        PostEntry post = postService.requireVisiblePost(postId, actor);
        return toDetail(post, actor);
    }

    @PostMapping("/posts/{postId}/likes")
    public ActionResponse likePost(@PathVariable long postId, HttpServletRequest request) {
        postService.addLike(postId, currentUser(request));
        return new ActionResponse("点赞成功");
    }

    @DeleteMapping("/posts/{postId}/likes")
    public ActionResponse unlikePost(@PathVariable long postId, HttpServletRequest request) {
        postService.removeLike(postId, currentUser(request));
        return new ActionResponse("取消点赞成功");
    }

    @PostMapping("/posts/{postId}/comments")
    public CommentResponse createComment(@PathVariable long postId, @Valid @RequestBody CreateCommentRequest request,
                                         HttpServletRequest httpRequest) {
        UserAccount actor = currentUser(httpRequest);
        CommentEntry comment = postService.addComment(
            postId,
            actor,
            request.content(),
            request.parentCommentId(),
            request.replyToUserId(),
            request.mentionUserIds()
        );
        List<Long> relatedUserIds = new ArrayList<>(comment.getMentionUserIds());
        if (comment.getReplyToUserId() != null) {
            relatedUserIds.add(comment.getReplyToUserId());
        }
        return toComment(comment, actor, dataStore.snapshotUsers(relatedUserIds));
    }

    @DeleteMapping("/comments/{commentId}")
    public ActionResponse deleteComment(@PathVariable long commentId, HttpServletRequest request) {
        postService.deleteComment(commentId, currentUser(request));
        return new ActionResponse("评论删除成功");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(SecurityException.class)
    public ErrorResponse handleSecurity(SecurityException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    private PostSummaryResponse toSummary(PostEntry post, UserAccount actor) {
        UserAccount author = dataStore.getUser(post.getAuthorId());
        PetProfile pet = post.getPetId() == null ? null : dataStore.findPet(post.getPetId()).orElse(null);
        return new PostSummaryResponse(
            post.getId(),
            post.getAuthorId(),
            post.getContent(),
            post.getImageUrls(),
            post.getVideoUrl(),
            post.getTopic(),
            post.getVisibility().name(),
            post.getVisibleCircleIds(),
            post.getReviewStatus().name(),
            post.getReviewReason(),
            post.getLikeCount(),
            post.getCommentCount(),
            dataStore.hasLiked(post.getId(), actor.getId()),
            author == null ? "未知用户" : author.getNickname(),
            pet == null ? null : pet.getName(),
            post.getCreatedAt().toString()
        );
    }

    private PostDetailResponse toDetail(PostEntry post, UserAccount actor) {
        UserAccount author = dataStore.getUser(post.getAuthorId());
        PetProfile pet = post.getPetId() == null ? null : dataStore.findPet(post.getPetId()).orElse(null);
        List<CommentEntry> comments = dataStore.listCommentsByPost(post.getId());
        List<Long> relatedUserIds = new ArrayList<>();
        for (CommentEntry comment : comments) {
            relatedUserIds.add(comment.getAuthorId());
            if (comment.getReplyToUserId() != null) {
                relatedUserIds.add(comment.getReplyToUserId());
            }
            relatedUserIds.addAll(comment.getMentionUserIds());
        }
        Map<Long, UserAccount> userSnapshot = dataStore.snapshotUsers(relatedUserIds);
        return new PostDetailResponse(
            post.getId(),
            post.getAuthorId(),
            post.getContent(),
            post.getImageUrls(),
            post.getVideoUrl(),
            post.getTopic(),
            post.getVisibility().name(),
            post.getVisibleCircleIds(),
            post.getReviewStatus().name(),
            post.getReviewReason(),
            post.getLikeCount(),
            post.getCommentCount(),
            dataStore.hasLiked(post.getId(), actor.getId()),
            author == null ? "未知用户" : author.getNickname(),
            pet == null ? null : pet.getName(),
            post.getCreatedAt().toString(),
            comments.stream().map(comment -> toComment(comment, userSnapshot.get(comment.getAuthorId()), userSnapshot)).toList()
        );
    }

    private CommentResponse toComment(CommentEntry comment, UserAccount author, Map<Long, UserAccount> userSnapshot) {
        List<String> mentionNicknames = comment.getMentionUserIds().stream()
            .map(userId -> {
                UserAccount mentioned = userSnapshot.get(userId);
                return mentioned == null ? "未知用户" : mentioned.getNickname();
            })
            .toList();
        UserAccount replyToUser = comment.getReplyToUserId() == null ? null : userSnapshot.get(comment.getReplyToUserId());
        return new CommentResponse(
            comment.getId(),
            comment.getAuthorId(),
            comment.getParentCommentId(),
            comment.getReplyToUserId(),
            comment.getMentionUserIds(),
            comment.getContent(),
            author == null ? "未知用户" : author.getNickname(),
            replyToUser == null ? null : replyToUser.getNickname(),
            mentionNicknames,
            comment.getCreatedAt().toString()
        );
    }

    private UserAccount currentUser(HttpServletRequest request) {
        return (UserAccount) request.getAttribute(AuthInterceptor.CURRENT_USER);
    }

    private PostVisibility parseVisibility(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PostVisibility.PUBLIC;
        }
        try {
            return PostVisibility.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("可见范围仅支持 PUBLIC 或 CIRCLE");
        }
    }

    public record CreatePostRequest(Long petId, @NotBlank String content, List<String> imageUrls, String videoUrl, String topic,
                                    String visibility, List<Long> circleIds) {
    }

    public record CreateCommentRequest(@NotBlank String content, Long parentCommentId, Long replyToUserId,
                                       List<Long> mentionUserIds) {
    }

    public record PostSummaryResponse(long id, long authorId, String content, List<String> imageUrls, String videoUrl, String topic,
                                      String visibility, List<Long> visibleCircleIds, String reviewStatus, String reviewReason,
                                      int likeCount, int commentCount, boolean likedByMe, String authorNickname, String petName,
                                      String createdAt) {
    }

    public record PostDetailResponse(long id, long authorId, String content, List<String> imageUrls, String videoUrl, String topic,
                                     String visibility, List<Long> visibleCircleIds, String reviewStatus, String reviewReason,
                                     int likeCount, int commentCount, boolean likedByMe, String authorNickname, String petName,
                                     String createdAt, List<CommentResponse> comments) {
    }

    public record CommentResponse(long id, long authorId, Long parentCommentId, Long replyToUserId, List<Long> mentionUserIds,
                                  String content, String authorNickname, String replyToNickname,
                                  List<String> mentionNicknames, String createdAt) {
    }

    public record ActionResponse(String message) {
    }

    public record ErrorResponse(String message) {
    }
}
