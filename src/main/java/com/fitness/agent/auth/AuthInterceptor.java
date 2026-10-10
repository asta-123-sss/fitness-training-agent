package com.fitness.agent.auth;

import com.fitness.agent.model.dto.Result;
import com.fitness.agent.service.AuthSessionService;
import com.fitness.agent.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权拦截器：从 Authorization: Bearer &lt;token&gt; 解析当前用户，未登录/失效返回 401。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionService sessionService;

    public AuthInterceptor(AuthSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String token = resolveToken(request);
        Long userId = token == null ? null : sessionService.resolveUserId(token);
        if (userId == null) {
            writeUnauthorized(response);
            return false;
        }
        UserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler, @Nullable Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JsonUtil.toJson(new Result<Void>(401, "未登录或登录已过期", null)));
    }
}
