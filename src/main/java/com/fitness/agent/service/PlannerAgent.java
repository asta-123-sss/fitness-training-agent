package com.fitness.agent.service;

import com.fitness.agent.agent.AgentToolCaller;
import com.fitness.agent.agent.JsonExtractor;
import com.fitness.agent.agent.JsonSchemaValidator;
import com.fitness.agent.config.AgentProperties;
import com.fitness.agent.config.PromptProperties;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.CriticVerdict;
import com.fitness.agent.model.Exercise;
import com.fitness.agent.model.UserProfile;
import com.fitness.agent.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 规划 Agent：基于用户画像 + RAG 检索到的动作库，生成一周训练计划。
 * 支持 Function Call（调用 BMI/热量工具），输出 JSON 并通过 Schema 校验。
 */
@Slf4j
@Component
public class PlannerAgent {

    private final AgentToolCaller toolCaller;
    private final PromptProperties prompts;
    private final AgentProperties properties;
    private final String schema;

    public PlannerAgent(AgentToolCaller toolCaller, PromptProperties prompts, AgentProperties properties) {
        this.toolCaller = toolCaller;
        this.prompts = prompts;
        this.properties = properties;
        this.schema = JsonUtil.readClasspath("schema/plan-schema.json");
    }

    /** 首次规划。 */
    public String plan(UserProfile profile, List<Exercise> exercises, String userInput, String context) {
        return generateWithRetry(buildUserMessage(profile, exercises, userInput, null, context));
    }

    /** 根据 Critic 反馈重新规划。 */
    public String replan(UserProfile profile, List<Exercise> exercises, String previousPlan, CriticVerdict verdict, String userInput, String context) {
        String feedback = "上一次计划未通过评审：\n问题：" + verdict.getIssues()
                + "\n建议：" + verdict.getSuggestions()
                + "\n请据此重新规划，务必规避以上问题。";
        return generateWithRetry(buildUserMessage(profile, exercises, userInput, feedback, context));
    }

    private String buildUserMessage(UserProfile profile, List<Exercise> exercises, String userInput, String extra, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户原始输入：\n").append(userInput == null ? "" : userInput).append("\n\n");
        sb.append("解析出的用户信息：\n").append(JsonUtil.toJson(profile)).append("\n\n");
        sb.append("可用动作库（已排除伤病禁忌与不可用器械）：\n").append(JsonUtil.toJson(exercises)).append("\n");
        if (context != null && !context.isBlank()) {
            sb.append("\n").append(context).append("\n");
        }
        if (extra != null) {
            sb.append("\n").append(extra);
        }
        return sb.toString();
    }

    private String generateWithRetry(String userMessage) {
        String current = userMessage;
        for (int attempt = 0; attempt < properties.getExtractorMaxRetries(); attempt++) {
            String raw = toolCaller.callWithTools(prompts.getPlanner(), current);
            String json = JsonExtractor.extractJson(raw);
            List<String> errors = json == null
                    ? List.of("输出中未找到 JSON 对象")
                    : JsonSchemaValidator.validate(json, schema);

            if (json != null && errors.isEmpty()) {
                log.info("训练计划生成成功");
                return json;
            }
            log.warn("规划第 {} 次未通过校验：{}", attempt + 1, errors);
            current = userMessage + "\n\n[修正要求] "
                    + prompts.getCorrection().replace("{{error}}", String.join("；", errors));
        }
        throw new BusinessException("训练计划生成失败，请稍后重试。");
    }
}
