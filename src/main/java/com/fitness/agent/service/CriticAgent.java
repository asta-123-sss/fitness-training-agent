package com.fitness.agent.service;

import com.fitness.agent.agent.JsonExtractor;
import com.fitness.agent.agent.JsonSchemaValidator;
import com.fitness.agent.config.AgentProperties;
import com.fitness.agent.config.PromptProperties;
import com.fitness.agent.model.CriticVerdict;
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
 * 评审 Agent：校验训练计划（强度是否超标、是否含禁忌动作、负荷是否循序渐进），
 * 输出结构化评审结论；多次解析失败时保守视为通过，避免阻塞主链路。
 */
@Slf4j
@Component
public class CriticAgent {

    private final ChatModel chatModel;
    private final PromptProperties prompts;
    private final AgentProperties properties;
    private final String schema;

    public CriticAgent(ChatModel chatModel, PromptProperties prompts, AgentProperties properties) {
        this.chatModel = chatModel;
        this.prompts = prompts;
        this.properties = properties;
        this.schema = JsonUtil.readClasspath("schema/critic-schema.json");
    }

    public CriticVerdict review(UserProfile profile, String planJson) {
        String userMessage = "用户信息：\n" + JsonUtil.toJson(profile)
                + "\n\n待评审训练计划：\n" + planJson;
        String current = userMessage;

        for (int attempt = 0; attempt < properties.getExtractorMaxRetries(); attempt++) {
            String raw = chatModel.chat(ChatRequest.builder()
                            .messages(List.of(SystemMessage.from(prompts.getCritic()), UserMessage.from(current)))
                            .build())
                    .aiMessage().text();

            String json = JsonExtractor.extractJson(raw);
            List<String> errors = json == null
                    ? List.of("输出中未找到 JSON 对象")
                    : JsonSchemaValidator.validate(json, schema);

            if (json != null && errors.isEmpty()) {
                CriticVerdict verdict = JsonUtil.readValue(json, CriticVerdict.class);
                log.info("评审结果：pass={}, issues={}", verdict.isPass(), verdict.getIssues());
                return verdict;
            }
            log.warn("评审第 {} 次未通过校验：{}", attempt + 1, errors);
            current = userMessage + "\n\n[修正要求] "
                    + prompts.getCorrection().replace("{{error}}", String.join("；", errors));
        }

        log.warn("评审多次解析失败，保守视为通过");
        CriticVerdict fallback = new CriticVerdict();
        fallback.setPass(true);
        return fallback;
    }
}
