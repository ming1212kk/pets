package com.pet.social.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserRole;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SECRET = "pet-social-stage1-secret-key";
    private static final long EXPIRES_IN_SECONDS = 7 * 24 * 60 * 60;

    private final ObjectMapper objectMapper;

    public JwtService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String issueToken(UserAccount user) {
        try {
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(objectMapper.writeValueAsBytes(Map.of(
                "sub", user.getId(),
                "role", user.getRole().name(),
                "exp", Instant.now().getEpochSecond() + EXPIRES_IN_SECONDS
            )));
            String signature = encode(sign(header + "." + payload));
            return header + "." + payload + "." + signature;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法生成 JWT", exception);
        }
    }

    public TokenPayload parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("token 格式错误");
        }
        String signingInput = parts[0] + "." + parts[1];
        if (!constantTimeEquals(parts[2], encode(sign(signingInput)))) {
            throw new IllegalArgumentException("token 签名无效");
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
            long userId = ((Number) payload.get("sub")).longValue();
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new IllegalArgumentException("token 已过期");
            }
            UserRole role = UserRole.valueOf(String.valueOf(payload.get("role")));
            return new TokenPayload(userId, role, Instant.ofEpochSecond(exp));
        } catch (IOException exception) {
            throw new IllegalArgumentException("token 无法解析", exception);
        }
    }

    private byte[] sign(String rawValue) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(rawValue.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("token 签名失败", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < left.length(); index++) {
            result |= left.charAt(index) ^ right.charAt(index);
        }
        return result == 0;
    }

    public record TokenPayload(long userId, UserRole role, Instant expiresAt) {
    }
}
