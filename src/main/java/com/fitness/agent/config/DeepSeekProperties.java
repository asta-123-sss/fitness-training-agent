package com.fitness.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 相关配置。密钥只从环境变量/配置文件读取，禁止在代码中硬编码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    /** DeepSeek API Key（来自环境变量 DEEPSEEK_API_KEY）。 */
    private String apiKey;

    /** OpenAI 兼容接口地址。 */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名称。 */
    private String model = "deepseek-chat";

    /** 采样温度。 */
    private double temperature = 0.3;

    /** 是否打印请求/响应日志。 */
    private boolean logRequests = false;
}
