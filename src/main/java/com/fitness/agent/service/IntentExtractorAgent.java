package com.fitness.agent.service;

import com.fitness.agent.agent.JsonExtractor;
import com.fitness.agent.agent.JsonSchemaValidator;
import com.fitness.agent.config.AgentProperties;
import com.fitness.agent.config.PromptProperties;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.UserProfile;
import com.fitness.agent.util.JsonUtil;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 意图解析 Agent：从用户自然语言中提取结构化健身信息，JSON Schema 校验失败时自动修正重试。
 */
@Slf4j
@Component
public class IntentExtractorAgent {

    private final ChatModel chatModel;
    private final PromptProperties prompts;
    private final AgentProperties properties;
    private final String schema;

    public IntentExtractorAgent(ChatModel chatModel, PromptProperties prompts, AgentProperties properties) {
        this.chatModel = chatModel;
        this.prompts = prompts;
        this.properties = properties;
        this.schema = JsonUtil.readClasspath("schema/profile-schema.json");
    }

    public UserProfile extract(String userInput) {
        return extract(userInput, null);
    }

    /**
     * 意图解析；knownProfileJson 为用户已保存的身体档案 JSON，用户未提及的字段优先采用档案值。
     */
    public UserProfile extract(String userInput, String knownProfileJson) {
        String baseInput = knownProfileJson == null || knownProfileJson.isBlank()
                ? userInput
                : userInput
                        + "\n\n[已知档案] 用户之前保存的身体档案（JSON）：\n" + knownProfileJson
                        + "\n用户未明确提及的字段，请优先采用已知档案中的值。";
        String current = baseInput;
        for (int attempt = 0; attempt < properties.getExtractorMaxRetries(); attempt++) {
            String raw = generate(prompts.getIntentExtractor(), current);
            String json = JsonExtractor.extractJson(raw);

            if (json == null) {
                log.warn("意图解析第 {} 次未返回合法 JSON", attempt + 1);
                current = correction(baseInput, "输出中未找到 JSON 对象");
                continue;
            }

            List<String> errors = JsonSchemaValidator.validate(json, schema);
            if (errors.isEmpty()) {
                UserProfile profile = JsonUtil.readValue(json, UserProfile.class);
                log.info("意图解析成功：{}", JsonUtil.toJson(profile));
                return profile;
            }
            log.warn("意图解析第 {} 次未通过 Schema 校验：{}", attempt + 1, errors);
            current = correction(baseInput, String.join("；", errors));
        }
        throw new BusinessException("未能准确理解您的信息，请更清晰地描述身高、体重、目标等关键信息。");
    }

    private String generate(String systemPrompt, String userMessage) {
        return chatModel.chat(ChatRequest.builder()
                        .messages(List.of(SystemMessage.from(systemPrompt), UserMessage.from(userMessage)))
                        .build())
                .aiMessage().text();
    }

    private String correction(String originalInput, String errorDetail) {
        return prompts.getCorrection().replace("{{error}}", errorDetail)
                + "\n\n用户原始输入：\n" + originalInput;
    }
}
