package com.fitness.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 会话缓存：缓存会话中间上下文（profile、plan），减少重复 LLM 调用。
 * 所有操作均做容错处理——Redis 不可用时不影响主链路（降级为纯 MySQL）。
 */
@Slf4j
@Component
public class SessionCacheService {

    private static final String KEY_PREFIX = "fitness:task:";
    private static final String FIELD_PROFILE = "profile";
    private static final String FIELD_PLAN = "plan";

    private final StringRedisTemplate redis;

    public SessionCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @NonNull
    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    public void putProfile(String sessionId, String profileJson) {
        if (profileJson == null) {
            return;
        }
        try {
            redis.opsForHash().put(key(sessionId), FIELD_PROFILE, profileJson);
            redis.expire(key(sessionId), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis 缓存 profile 失败（忽略）：{}", e.getMessage());
        }
    }

    public String getProfile(String sessionId) {
        try {
            Object v = redis.opsForHash().get(key(sessionId), FIELD_PROFILE);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            log.warn("Redis 读取 profile 失败（忽略）：{}", e.getMessage());
            return null;
        }
    }

    public void putPlan(String sessionId, String planJson) {
        if (planJson == null) {
            return;
        }
        try {
            redis.opsForHash().put(key(sessionId), FIELD_PLAN, planJson);
            redis.expire(key(sessionId), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis 缓存 plan 失败（忽略）：{}", e.getMessage());
        }
    }

    public void clear(String sessionId) {
        try {
            redis.delete(key(sessionId));
        } catch (Exception e) {
            log.warn("Redis 清理失败（忽略）：{}", e.getMessage());
        }
    }
}
