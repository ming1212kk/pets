package com.pet.social.controller;

import com.pet.social.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sms/send")
    public SmsCodeResponse sendSms(@Valid @RequestBody SmsCodeRequest request) {
        AuthService.SmsCodeTicket ticket = authService.sendSmsCode(request.phone());
        return new SmsCodeResponse(ticket.code(), ticket.expiresAt().toString(), ticket.cooldownSeconds());
    }

    @PostMapping("/login/phone")
    public AuthResponse loginByPhone(@Valid @RequestBody PhoneLoginRequest request) {
        return toResponse(authService.loginByPhone(request.phone(), request.code()));
    }

    @PostMapping("/login/wechat")
    public AuthResponse loginByWechat(@Valid @RequestBody WechatLoginRequest request) {
        return toResponse(authService.loginByWechat(request.code(), request.nickname(), request.avatarUrl()));
    }

    @PostMapping("/admin/login")
    public AuthResponse loginAsAdmin(@Valid @RequestBody AdminLoginRequest request) {
        return toResponse(authService.loginAsAdmin(request.username(), request.password()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    private AuthResponse toResponse(AuthService.AuthResult result) {
        return new AuthResponse(
            result.user().getId(),
            result.user().getNickname(),
            result.user().getAvatarUrl(),
            result.user().getPhone(),
            result.user().getRole().name(),
            result.user().getWechatKey() != null && !result.user().getWechatKey().isBlank(),
            result.token()
        );
    }

    public record SmsCodeRequest(@NotBlank String phone) {
    }

    public record PhoneLoginRequest(@NotBlank String phone, @NotBlank String code) {
    }

    public record WechatLoginRequest(@NotBlank String code, String nickname, String avatarUrl) {
    }

    public record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record SmsCodeResponse(String debugCode, String expiresAt, long cooldownSeconds) {
    }

    public record AuthResponse(long userId, String nickname, String avatarUrl, String phone, String role,
                               boolean wechatBound, String token) {
    }

    public record ErrorResponse(String message) {
    }
}
