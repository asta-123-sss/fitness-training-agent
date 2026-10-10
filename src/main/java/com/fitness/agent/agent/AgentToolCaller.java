package com.fitness.agent.agent;

import com.fitness.agent.config.AgentProperties;
import com.fitness.agent.tool.AgentTool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Function Call 调用执行器：把 SPI 工具转成 LangChain4j {@link ToolSpecification}，
 * 并完成「调用 → 执行 → 回传结果」的循环，直到模型给出最终文本或达到轮数上限。
 */
@Slf4j
@Component
public class AgentToolCaller {

    private final ChatModel chatModel;
    private final AgentProperties properties;
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public AgentToolCaller(ChatModel chatModel, AgentProperties properties) {
        this.chatModel = chatModel;
        this.properties = properties;
        // 通过 Java SPI 自动发现所有可插拔工具
        ServiceLoader<AgentTool> loader = ServiceLoader.load(AgentTool.class);
        for (AgentTool tool : loader) {
            this.tools.put(tool.name(), tool);
        }
        log.info("通过 SPI 加载工具：{}", this.tools.keySet());
    }

    /**
     * 生成工具规格列表，供 DeepSeek Function Call 使用。
     */
    public List<ToolSpecification> toolSpecifications() {
        List<ToolSpecification> specs = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            specs.add(ToolSpecification.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .parameters(tool.parameters())
                    .build());
        }
        return specs;
    }

    /**
     * 带工具调用的对话：返回模型最终文本（可能是纯文本，也可能经历若干次工具调用）。
     */
    public String callWithTools(String systemPrompt, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from(userMessage));

        List<ToolSpecification> specs = toolSpecifications();

        for (int i = 0; i < properties.getMaxToolCalls(); i++) {
            ChatResponse response = chatModel.chat(ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(specs)
                    .build());
            AiMessage aiMessage = response.aiMessage();

            if (!aiMessage.hasToolExecutionRequests()) {
                return aiMessage.text();
            }

            // 把 assistant 的工具调用消息加入上下文，再逐条执行并回传结果
            messages.add(aiMessage);
            for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                String result = executeTool(request);
                messages.add(ToolExecutionResultMessage.from(request, result));
            }
        }

        log.warn("工具调用轮数超过上限 {}", properties.getMaxToolCalls());
        return "工具调用次数过多，请稍后重试。";
    }

    private String executeTool(ToolExecutionRequest request) {
        AgentTool tool = tools.get(request.name());
        if (tool == null) {
            return "{\"error\":\"未找到工具: " + request.name() + "\"}";
        }
        try {
            log.info("执行工具：{}，参数：{}", request.name(), request.arguments());
            return tool.execute(request.arguments());
        } catch (Exception e) {
            log.error("工具执行失败：{}", request.name(), e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
