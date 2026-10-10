package com.fitness.agent.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 配置：把 OpenAI 兼容客户端指向 DeepSeek 在线 API。
 *
 * <p>DeepSeek 提供 OpenAI 兼容接口（含 Function Call），因此复用 LangChain4j 的
 * {@link OpenAiChatModel}，仅需替换 baseUrl / modelName / apiKey，不引入其它大模型 SDK。</p>
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    ChatModel chatLanguageModel(DeepSeekProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(120))
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogRequests())
                .build();
    }
}
