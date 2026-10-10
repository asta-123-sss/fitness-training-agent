package com.fitness.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录会话与扫码会话的 Redis 存取。容错写法与 SessionCacheService 一致：
 * Redis 不可用时登录会失败，但不会把异常抛穿主链路。
 */
@Slf4j
@Component
public class AuthSessionService {

    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String QR_PREFIX = "auth:qr:";

    private static final long TOKEN_TTL_SECONDS = 7L * 24 * 60 * 60; // token 有效期 7 天
    private static final long QR_TTL_SECONDS = 5L * 60;              // 二维码会话 5 分钟

    private static final String QR_CONFIRMED_PREFIX = "CONFIRMED:";

    private final StringRedisTemplate redis;

    public AuthSessionService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 创建登录会话，返回 token。 */
    public String createToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(TOKEN_PREFIX + token, userId + "", TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        return token;
    }

    /** 解析 token 对应的 userId，不存在/失效返回 null。 */
    public Long resolveUserId(String token) {
        try {
            String v = redis.opsForValue().get(TOKEN_PREFIX + token);
            return v == null ? null : Long.valueOf(v);
        } catch (Exception e) {
            log.warn("Redis 读取登录会话失败（忽略）：{}", e.getMessage());
            return null;
        }
    }

    /** 删除登录会话（退出登录）。 */
    public void removeToken(String token) {
        try {
            redis.delete(TOKEN_PREFIX + token);
        } catch (Exception e) {
            log.warn("Redis 删除登录会话失败（忽略）：{}", e.getMessage());
        }
    }

    /** 创建扫码会话，返回 qrId。 */
    public String createQr() {
        String qrId = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(QR_PREFIX + qrId, "PENDING", QR_TTL_SECONDS, TimeUnit.SECONDS);
        return qrId;
    }

    /** 扫码会话原始状态：PENDING / CONFIRMED:userId / null（不存在或已过期）。 */
    public String qrRaw(String qrId) {
        try {
            return redis.opsForValue().get(QR_PREFIX + qrId);
        } catch (Exception e) {
            log.warn("Redis 读取扫码会话失败（忽略）：{}", e.getMessage());
            return null;
        }
    }

    /** 手机端确认：把扫码会话标记为已确认并绑定 userId。 */
    public void confirmQr(String qrId, Long userId) {
        redis.opsForValue().set(QR_PREFIX + qrId, QR_CONFIRMED_PREFIX + userId, QR_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 电脑端原子认领：仅当状态为 CONFIRMED 且成功删除 key 时返回 userId，
     * 保证多个并发轮询里只有一个能领取到登录态。
     */
    public Long claimQr(String qrId) {
        try {
            String v = redis.opsForValue().get(QR_PREFIX + qrId);
            if (v == null || !v.startsWith(QR_CONFIRMED_PREFIX)) {
                return null;
            }
            Boolean deleted = redis.delete(QR_PREFIX + qrId);
            if (Boolean.TRUE.equals(deleted)) {
                return Long.valueOf(v.substring(QR_CONFIRMED_PREFIX.length()));
            }
            return null;
        } catch (Exception e) {
            log.warn("Redis 认领扫码会话失败（忽略）：{}", e.getMessage());
            return null;
        }
    }
}
