package com.pet.social.controller;

import com.pet.social.config.AuthInterceptor;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserRole;
import com.pet.social.domain.UserStatus;
import com.pet.social.service.PostService;
import com.pet.social.store.AppDataStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AppDataStore dataStore;
    private final PostService postService;

    public AdminController(AppDataStore dataStore, PostService postService) {
        this.dataStore = dataStore;
        this.postService = postService;
    }

    @GetMapping("/posts")
    public List<AdminPostResponse> listPosts(HttpServletRequest request) {
        requireAdmin(request);
        return dataStore.listAllPosts().stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/posts/{postId}")
    public ActionResponse deletePost(@PathVariable long postId, HttpServletRequest request) {
        requireAdmin(request);
        postService.deletePostAsAdmin(postId);
        return new ActionResponse("动态已删除");
    }

    @PostMapping("/users/{userId}/ban")
    public ActionResponse banUser(@PathVariable long userId, HttpServletRequest request) {
        requireAdmin(request);
        dataStore.updateUserStatus(userId, UserStatus.BANNED);
        return new ActionResponse("用户已封禁");
    }

    @PostMapping("/users/{userId}/unban")
    public ActionResponse unbanUser(@PathVariable long userId, HttpServletRequest request) {
        requireAdmin(request);
        dataStore.updateUserStatus(userId, UserStatus.ACTIVE);
        return new ActionResponse("用户已解封");
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

    private AdminPostResponse toResponse(PostEntry post) {
        UserAccount author = dataStore.getUser(post.getAuthorId());
        return new AdminPostResponse(
            post.getId(),
            author == null ? "未知用户" : author.getNickname(),
            post.getContent(),
            post.getReviewStatus().name(),
            post.getReviewReason(),
            post.getLikeCount(),
            post.getCommentCount(),
            post.getCreatedAt().toString()
        );
    }

    private void requireAdmin(HttpServletRequest request) {
        UserAccount actor = (UserAccount) request.getAttribute(AuthInterceptor.CURRENT_USER);
        if (actor == null || actor.getRole() != UserRole.ADMIN) {
            throw new SecurityException("需要管理员权限");
        }
    }

    public record AdminPostResponse(long id, String authorNickname, String content, String reviewStatus, String reviewReason,
                                    int likeCount, int commentCount, String createdAt) {
    }

    public record ActionResponse(String message) {
    }

    public record ErrorResponse(String message) {
    }
}
