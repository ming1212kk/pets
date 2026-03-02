package com.pet.social.config;

import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserStatus;
import com.pet.social.service.JwtService;
import com.pet.social.store.InMemoryDataStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER = "currentUser";

    private final JwtService jwtService;
    private final InMemoryDataStore dataStore;

    public AuthInterceptor(JwtService jwtService, InMemoryDataStore dataStore) {
        this.jwtService = jwtService;
        this.dataStore = dataStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth/")) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "缺少 Bearer token");
            return false;
        }
        try {
            JwtService.TokenPayload payload = jwtService.parse(authorization.substring(7));
            UserAccount user = dataStore.getUser(payload.userId());
            if (user == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "账号不存在");
                return false;
            }
            if (user.getStatus() == UserStatus.BANNED) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "账号已被封禁");
                return false;
            }
            request.setAttribute(CURRENT_USER, user);
            return true;
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
            return false;
        }
    }
}
