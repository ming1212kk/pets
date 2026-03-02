package com.pet.social.service;

import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserStatus;
import com.pet.social.store.InMemoryDataStore;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SMS_SEND_INTERVAL = Duration.ofSeconds(60);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final InMemoryDataStore dataStore;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SmsCodeState> smsCodes = new ConcurrentHashMap<>();

    public AuthService(InMemoryDataStore dataStore, JwtService jwtService) {
        this.dataStore = dataStore;
        this.jwtService = jwtService;
    }

    public SmsCodeTicket sendSmsCode(String phone) {
        SmsCodeState existing = smsCodes.get(phone);
        if (existing != null && Duration.between(existing.sentAt(), Instant.now()).compareTo(SMS_SEND_INTERVAL) < 0) {
            long waitSeconds = SMS_SEND_INTERVAL.minus(Duration.between(existing.sentAt(), Instant.now())).getSeconds();
            throw new IllegalArgumentException("验证码发送过于频繁，请在 " + waitSeconds + " 秒后重试");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(SMS_CODE_TTL);
        smsCodes.put(phone, new SmsCodeState(code, Instant.now(), expiresAt));
        return new SmsCodeTicket(code, expiresAt, SMS_SEND_INTERVAL.getSeconds());
    }

    public AuthResult loginByPhone(String phone, String code) {
        SmsCodeState state = smsCodes.get(phone);
        if (state == null || !state.code().equals(code) || Instant.now().isAfter(state.expiresAt())) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }
        UserAccount user = dataStore.getOrCreatePhoneUser(phone);
        assertActive(user);
        return new AuthResult(user, jwtService.issueToken(user));
    }

    public AuthResult loginByWechat(String code, String nickname) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("微信登录 code 不能为空");
        }
        UserAccount user = dataStore.getOrCreateWechatUser("wx:" + code.trim(), nickname);
        assertActive(user);
        return new AuthResult(user, jwtService.issueToken(user));
    }

    public AuthResult loginAsAdmin(String username, String password) {
        if (!ADMIN_USERNAME.equals(username) || !ADMIN_PASSWORD.equals(password)) {
            throw new IllegalArgumentException("管理员账号或密码错误");
        }
        UserAccount admin = dataStore.getOrCreateAdminUser();
        assertActive(admin);
        return new AuthResult(admin, jwtService.issueToken(admin));
    }

    private void assertActive(UserAccount user) {
        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalArgumentException("当前账号已被封禁");
        }
    }

    private record SmsCodeState(String code, Instant sentAt, Instant expiresAt) {
    }

    public record SmsCodeTicket(String code, Instant expiresAt, long cooldownSeconds) {
    }

    public record AuthResult(UserAccount user, String token) {
    }
}
