package com.fitness.agent.service;

import com.fitness.agent.agent.JsonExtractor;
import com.fitness.agent.config.PromptProperties;
import com.fitness.agent.model.IntentType;
import com.fitness.agent.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天 / 意图分类 Agent：在流水线入口判断用户输入是「训练规划」还是「日常聊天」。
 * 日常聊天直接友好回复，训练规划则交给后续的规划流水线。
 */
@Slf4j
@Component
public class ChatAgent {

    private final ChatModel chatModel;
    private final PromptProperties prompts;

    public ChatAgent(ChatModel chatModel, PromptProperties prompts) {
        this.chatModel = chatModel;
        this.prompts = prompts;
    }

    /**
     * 判定意图。分类失败（如未返回合法 JSON）时回退为 PLAN，
     * 保证「拿不准就按训练请求处理」不影响原有训练功能。
     */
    public IntentType classify(String userInput) {
        try {
            String raw = generate(prompts.getIntentClassifier(), userInput);
            String json = JsonExtractor.extractJson(raw);
            if (json == null) {
                log.warn("意图分类未返回 JSON，回退为 PLAN");
                return IntentType.PLAN;
            }
            JsonNode node = JsonUtil.readTree(json);
            String type = node.path("type").asText("");
            if ("CHAT".equalsIgnoreCase(type)) {
                return IntentType.CHAT;
            }
            if ("QNA".equalsIgnoreCase(type)) {
                return IntentType.QNA;
            }
            return IntentType.PLAN;
        } catch (Exception e) {
            log.warn("意图分类失败，回退为 PLAN：{}", e.getMessage());
            return IntentType.PLAN;
        }
    }

    /** 日常聊天：直接生成友好回复。 */
    public String reply(String userInput) {
        return generate(prompts.getChat(), userInput);
    }

    /** 健身/营养知识问答：直接给出专业解答。 */
    public String qa(String userInput) {
        return generate(prompts.getQa(), userInput);
    }

    private String generate(String systemPrompt, String userMessage) {
        return chatModel.chat(ChatRequest.builder()
                        .messages(List.of(SystemMessage.from(systemPrompt), UserMessage.from(userMessage)))
                        .build())
                .aiMessage().text();
    }
}
