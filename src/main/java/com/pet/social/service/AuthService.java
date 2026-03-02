package com.pet.social.service;

import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserStatus;
import com.pet.social.store.AppDataStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {
    private static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SMS_SEND_INTERVAL = Duration.ofSeconds(60);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final AppDataStore dataStore;
    private final JwtService jwtService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppDataStore dataStore, JwtService jwtService, StringRedisTemplate stringRedisTemplate) {
        this.dataStore = dataStore;
        this.jwtService = jwtService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public SmsCodeTicket sendSmsCode(String phone) {
        String cooldownKey = cooldownKey(phone);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            Long waitSeconds = stringRedisTemplate.getExpire(cooldownKey);
            long remaining = waitSeconds == null || waitSeconds < 0 ? SMS_SEND_INTERVAL.getSeconds() : waitSeconds;
            throw new IllegalArgumentException("验证码发送过于频繁，请在 " + remaining + " 秒后重试");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(SMS_CODE_TTL);
        stringRedisTemplate.opsForValue().set(codeKey(phone), code, SMS_CODE_TTL);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", SMS_SEND_INTERVAL);
        return new SmsCodeTicket(code, expiresAt, SMS_SEND_INTERVAL.getSeconds());
    }

    public AuthResult loginByPhone(String phone, String code) {
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey(phone));
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }
        stringRedisTemplate.delete(codeKey(phone));
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

    private String codeKey(String phone) {
        return "auth:sms:code:" + phone;
    }

    private String cooldownKey(String phone) {
        return "auth:sms:cooldown:" + phone;
    }

    public record SmsCodeTicket(String code, Instant expiresAt, long cooldownSeconds) {
    }

    public record AuthResult(UserAccount user, String token) {
    }
}
