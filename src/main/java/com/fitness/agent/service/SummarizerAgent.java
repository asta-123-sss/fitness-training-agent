package com.fitness.agent.service;

import com.fitness.agent.config.PromptProperties;
import com.fitness.agent.model.UserProfile;
import com.fitness.agent.util.JsonUtil;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 汇总 Agent：把评审通过的训练计划整理成结构化 Markdown 方案。
 */
@Component
public class SummarizerAgent {

    private final ChatModel chatModel;
    private final PromptProperties prompts;

    public SummarizerAgent(ChatModel chatModel, PromptProperties prompts) {
        this.chatModel = chatModel;
        this.prompts = prompts;
    }

    public String summarize(UserProfile profile, String planJson, String userInput, String context) {
        String userMessage = "用户原始输入：\n" + (userInput == null ? "" : userInput)
                + "\n\n用户信息：\n" + JsonUtil.toJson(profile)
                + (context == null || context.isBlank() ? "" : "\n\n" + context)
                + "\n\n训练计划：\n" + planJson
                + "\n\n请输出整理后的 Markdown 训练方案。";
        return chatModel.chat(ChatRequest.builder()
                        .messages(List.of(SystemMessage.from(prompts.getSummarizer()), UserMessage.from(userMessage)))
                        .build())
                .aiMessage().text();
    }
}
