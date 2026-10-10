package com.fitness.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 各角色提示词模板，全部从 prompts.yml 读取，方便修改而无需改代码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "prompts")
public class PromptProperties {

    /** 意图解析 Agent 的 system prompt。 */
    private String intentExtractor;

    /** 意图分类（训练规划 / 日常聊天）的 system prompt。 */
    private String intentClassifier;

    /** 日常聊天回复的 system prompt。 */
    private String chat;

    /** 健身/营养知识问答回复的 system prompt。 */
    private String qa;

    /** 规划 Agent 的 system prompt。 */
    private String planner;

    /** 评审 Agent 的 system prompt。 */
    private String critic;

    /** 汇总 Agent 的 system prompt。 */
    private String summarizer;

    /** JSON 校验失败时的修正 prompt 模板，占位符 {{error}}。 */
    private String correction;
}
