package com.fitness.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fitness.agent.util.JsonUtil;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 每日热量推荐工具：基于 Mifflin-St Jeor 公式计算基础代谢 BMR、总消耗 TDEE 与目标热量。
 */
public class CalorieRecommendationTool implements AgentTool {

    @Override
    public String name() {
        return "recommend_calories";
    }

    @Override
    public String description() {
        return "根据性别、身高、体重、年龄和健身目标，计算每日推荐热量（基础代谢 BMR、总消耗 TDEE、目标热量）。";
    }

    @Override
    public JsonObjectSchema parameters() {
        return JsonObjectSchema.builder()
                .addStringProperty("gender", "性别，只能是 男 或 女")
                .addNumberProperty("heightCm", "身高，单位厘米")
                .addNumberProperty("weightKg", "体重，单位公斤")
                .addIntegerProperty("age", "年龄")
                .addStringProperty("goal", "健身目标：增肌 / 减脂 / 维持")
                .required("gender", "heightCm", "weightKg", "age", "goal")
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        JsonNode node = JsonUtil.readTree(argumentsJson);
        String gender = node.path("gender").asText();
        double heightCm = node.path("heightCm").asDouble();
        double weightKg = node.path("weightKg").asDouble();
        int age = node.path("age").asInt();
        String goal = node.path("goal").asText();

        // Mifflin-St Jeor 基础代谢公式
        double bmr;
        if ("女".equals(gender)) {
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
        } else {
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + 5;
        }

        // 轻度活动系数 1.375
        double tdee = bmr * 1.375;

        // 目标热量调整
        double target = switch (goal) {
            case "增肌" -> tdee + 300;
            case "减脂" -> tdee - 400;
            default -> tdee;
        };

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bmr", Math.round(bmr));
        result.put("tdee", Math.round(tdee));
        result.put("targetCalories", Math.round(target));
        return JsonUtil.toJson(result);
    }
}
