package com.fitness.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 流水线可调参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** Critic 评审不通过时的最大重生成次数（防止死循环）。 */
    private int maxCriticIterations = 3;

    /** LLM 输出非法 JSON 时的修正重试次数。 */
    private int extractorMaxRetries = 3;

    /** RAG 词法检索返回的候选动作数量。 */
    private int retrievalTopK = 8;

    /** 单次规划允许的最大工具调用轮数。 */
    private int maxToolCalls = 4;
}
