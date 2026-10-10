package com.fitness.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class UploadProperties {

    /** 上传目录（相对或绝对路径）。 */
    private String uploadDir = "uploads";

    /** 单文件大小上限（字节），默认 10MB。 */
    private long uploadMaxSize = 10485760;
}
