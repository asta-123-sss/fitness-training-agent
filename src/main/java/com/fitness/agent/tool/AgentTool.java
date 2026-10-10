package com.fitness.agent.tool;

import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

/**
 * 可插拔 Agent 工具的 Java SPI 接口。
 *
 * <p>实现类只需放在 classpath 并在 {@code META-INF/services/com.fitness.agent.tool.AgentTool}
 * 中登记，即可被 {@link com.fitness.agent.agent.AgentToolCaller} 通过 {@link java.util.ServiceLoader}
 * 自动发现并转成 LangChain4j 的 Function Call 工具规格。</p>
 */
public interface AgentTool {

    /** 工具名称（LLM 调用时使用）。 */
    String name();

    /** 工具描述，帮助 LLM 判断何时调用。 */
    String description();

    /** 参数 JSON Schema（LangChain4j 类型）。 */
    JsonObjectSchema parameters();

    /** 执行工具，入参为 LLM 给出的 JSON 字符串，返回 JSON 字符串结果。 */
    String execute(String argumentsJson);
}
