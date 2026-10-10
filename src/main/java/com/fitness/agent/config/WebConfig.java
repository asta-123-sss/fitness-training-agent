package com.fitness.agent.config;

import com.fitness.agent.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置：注册鉴权拦截器、声明需要登录的接口路径，并开放上传目录的静态访问。
 * 登录/注册、扫码创建/扫码状态、只读分享接口与静态资源保持公开。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @NonNull
    private final AuthInterceptor authInterceptor;

    private final String uploadLocation;

    public WebConfig(@NonNull AuthInterceptor authInterceptor, UploadProperties properties) {
        this.authInterceptor = authInterceptor;
        this.uploadLocation = toLocation(properties.getUploadDir());
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/upload",            // 文件上传（需登录）
                        "/api/task/*",            // submit / list / {sessionId} 进度 / DELETE
                        "/api/task/*/result",
                        "/api/task/*/pin",
                        "/api/task/*/rename",
                        "/api/task/*/archive",
                        "/api/task/*/move",
                        "/api/project",           // POST 新建项目
                        "/api/project/*",         // GET 项目列表
                        "/api/conversation",      // POST 新建对话
                        "/api/conversation/*",    // list / {conversationId} 详情 / DELETE
                        "/api/conversation/*/pin",
                        "/api/conversation/*/rename",
                        "/api/conversation/*/archive",
                        "/api/conversation/*/move",
                        "/api/body",              // 身体指标计算器
                        "/api/body/**",
                        "/api/diet",              // 饮食工具
                        "/api/diet/**",
                        "/api/workout",           // 训练打卡日志
                        "/api/workout/**",
                        "/api/auth/logout",
                        "/api/auth/me",
                        "/api/auth/qr/confirm"
                );
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }

    private static String toLocation(String uploadDir) {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
