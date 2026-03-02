package com.pet.social.controller;

import com.pet.social.config.AuthInterceptor;
import com.pet.social.domain.UserAccount;
import com.pet.social.service.AuthService;
import com.pet.social.store.AppDataStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final AppDataStore dataStore;
    private final AuthService authService;

    public UserController(AppDataStore dataStore, AuthService authService) {
        this.dataStore = dataStore;
        this.authService = authService;
    }

    @GetMapping
    public UserProfileResponse getProfile(HttpServletRequest request) {
        return toResponse(currentUser(request));
    }

    @PutMapping
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpServletRequest httpRequest) {
        UserAccount user = currentUser(httpRequest);
        return toResponse(dataStore.updateUserProfile(user.getId(), request.nickname(), request.avatarUrl()));
    }

    @PostMapping("/wechat-bind")
    public UserProfileResponse bindWechat(@Valid @RequestBody BindWechatRequest request, HttpServletRequest httpRequest) {
        UserAccount user = currentUser(httpRequest);
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("微信绑定凭证不能为空");
        }
        return toResponse(dataStore.bindWechatUser(user.getId(), "wx:" + request.code().trim(), request.nickname(), request.avatarUrl()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    private UserProfileResponse toResponse(UserAccount user) {
        return new UserProfileResponse(
            user.getId(),
            user.getNickname(),
            user.getAvatarUrl(),
            user.getPhone(),
            user.getRole().name(),
            user.getWechatKey() != null && !user.getWechatKey().isBlank(),
            user.getCreatedAt().toString(),
            authService.issueToken(user)
        );
    }

    private UserAccount currentUser(HttpServletRequest request) {
        return (UserAccount) request.getAttribute(AuthInterceptor.CURRENT_USER);
    }

    public record UpdateProfileRequest(@NotBlank String nickname, String avatarUrl) {
    }

    public record BindWechatRequest(@NotBlank String code, String nickname, String avatarUrl) {
    }

    public record UserProfileResponse(long userId, String nickname, String avatarUrl, String phone, String role,
                                      boolean wechatBound, String createdAt, String token) {
    }

    public record ErrorResponse(String message) {
    }
}
