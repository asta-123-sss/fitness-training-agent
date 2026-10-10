package com.fitness.agent.auth;

/**
 * 当前登录用户上下文：由 AuthInterceptor 写入，请求结束后清理。
 */
public final class UserContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    /** 返回当前登录用户 id，未登录返回 null。 */
    public static Long get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
