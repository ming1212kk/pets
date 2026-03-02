package com.pet.social.controller;

import com.pet.social.config.AuthInterceptor;
import com.pet.social.domain.CommentEntry;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.UserAccount;
import com.pet.social.service.PostService;
import com.pet.social.store.InMemoryDataStore;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PostController {
    private final PostService postService;
    private final InMemoryDataStore dataStore;

    public PostController(PostService postService, InMemoryDataStore dataStore) {
        this.postService = postService;
        this.dataStore = dataStore;
    }

    @PostMapping("/posts")
    public PostDetailResponse createPost(@Valid @RequestBody CreatePostRequest request, HttpServletRequest httpRequest) {
        UserAccount actor = currentUser(httpRequest);
        List<String> images = request.imageUrls() == null ? Collections.emptyList() : request.imageUrls();
        PostEntry post = postService.createPost(actor, request.petId(), request.content(), images, request.videoUrl(), request.topic());
        return toDetail(post, actor);
    }

    @GetMapping("/posts")
    public List<PostSummaryResponse> listPublicPosts(HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        return postService.listPublicPosts().stream().map(post -> toSummary(post, actor)).toList();
    }

    @GetMapping("/posts/{postId}")
    public PostDetailResponse getPost(@PathVariable long postId, HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        PostEntry post = postService.requireVisiblePost(postId);
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
        CommentEntry comment = postService.addComment(postId, actor, request.content());
        return toComment(comment, actor);
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

    private PostSummaryResponse toSummary(PostEntry post, UserAccount actor) {
        UserAccount author = dataStore.getUser(post.getAuthorId());
        PetProfile pet = post.getPetId() == null ? null : dataStore.findPet(post.getPetId()).orElse(null);
        return new PostSummaryResponse(
            post.getId(),
            post.getContent(),
            post.getImageUrls(),
            post.getVideoUrl(),
            post.getTopic(),
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
        List<Long> authorIds = comments.stream().map(CommentEntry::getAuthorId).toList();
        Map<Long, UserAccount> userSnapshot = dataStore.snapshotUsers(authorIds);
        return new PostDetailResponse(
            post.getId(),
            post.getContent(),
            post.getImageUrls(),
            post.getVideoUrl(),
            post.getTopic(),
            post.getReviewStatus().name(),
            post.getReviewReason(),
            post.getLikeCount(),
            post.getCommentCount(),
            dataStore.hasLiked(post.getId(), actor.getId()),
            author == null ? "未知用户" : author.getNickname(),
            pet == null ? null : pet.getName(),
            post.getCreatedAt().toString(),
            comments.stream().map(comment -> {
                UserAccount commentAuthor = userSnapshot.get(comment.getAuthorId());
                return new CommentResponse(
                    comment.getId(),
                    comment.getContent(),
                    commentAuthor == null ? "未知用户" : commentAuthor.getNickname(),
                    comment.getCreatedAt().toString()
                );
            }).toList()
        );
    }

    private CommentResponse toComment(CommentEntry comment, UserAccount actor) {
        return new CommentResponse(comment.getId(), comment.getContent(), actor.getNickname(), comment.getCreatedAt().toString());
    }

    private UserAccount currentUser(HttpServletRequest request) {
        return (UserAccount) request.getAttribute(AuthInterceptor.CURRENT_USER);
    }

    public record CreatePostRequest(Long petId, @NotBlank String content, List<String> imageUrls, String videoUrl, String topic) {
    }

    public record CreateCommentRequest(@NotBlank String content) {
    }

    public record PostSummaryResponse(long id, String content, List<String> imageUrls, String videoUrl, String topic,
                                      String reviewStatus, String reviewReason, int likeCount, int commentCount,
                                      boolean likedByMe, String authorNickname, String petName, String createdAt) {
    }

    public record PostDetailResponse(long id, String content, List<String> imageUrls, String videoUrl, String topic,
                                     String reviewStatus, String reviewReason, int likeCount, int commentCount,
                                     boolean likedByMe, String authorNickname, String petName, String createdAt,
                                     List<CommentResponse> comments) {
    }

    public record CommentResponse(long id, String content, String authorNickname, String createdAt) {
    }

    public record ActionResponse(String message) {
    }

    public record ErrorResponse(String message) {
    }
}
