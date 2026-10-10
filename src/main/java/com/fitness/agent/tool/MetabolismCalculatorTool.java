package com.fitness.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fitness.agent.service.NutritionCalculator;
import com.fitness.agent.util.JsonUtil;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代谢计算工具：计算 BMI、BMR、TDEE，以及减脂/维持/增肌三档推荐热量与三大营养素。
 */
public class MetabolismCalculatorTool implements AgentTool {

    @Override
    public String name() {
        return "calculate_metabolism";
    }

    @Override
    public String description() {
        return "根据性别、年龄、身高、体重和日常活动量，计算 BMI、基础代谢 BMR、总消耗 TDEE，"
                + "以及减脂/维持/增肌三种目标各自的推荐热量与蛋白质、碳水、脂肪克数。";
    }

    @Override
    public JsonObjectSchema parameters() {
        return JsonObjectSchema.builder()
                .addStringProperty("gender", "性别，只能是 男 或 女")
                .addIntegerProperty("age", "年龄")
                .addNumberProperty("heightCm", "身高，单位厘米")
                .addNumberProperty("weightKg", "体重，单位公斤")
                .addStringProperty("activityLevel", "日常活动量：久坐 / 轻度 / 中度 / 高度 / 极高")
                .required("gender", "age", "heightCm", "weightKg")
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        JsonNode node = JsonUtil.readTree(argumentsJson);
        String gender = node.path("gender").asText();
        int age = node.path("age").asInt();
        double heightCm = node.path("heightCm").asDouble();
        double weightKg = node.path("weightKg").asDouble();
        String activityLevel = node.hasNonNull("activityLevel") ? node.path("activityLevel").asText() : "轻度";

        double bmi = NutritionCalculator.bmi(heightCm, weightKg);
        double bmr = NutritionCalculator.bmr(gender, age, heightCm, weightKg);
        double tdee = NutritionCalculator.tdee(bmr, activityLevel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bmi", round2(bmi));
        result.put("bmiCategory", NutritionCalculator.bmiCategory(bmi));
        result.put("bmr", Math.round(bmr));
        result.put("tdee", Math.round(tdee));

        Map<String, Object> targets = new LinkedHashMap<>();
        for (String goal : List.of("减脂", "维持", "增肌")) {
            int calories = NutritionCalculator.targetCalories(tdee, goal);
            NutritionCalculator.Macros m = NutritionCalculator.macros(goal, weightKg, calories);
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("calories", calories);
            t.put("protein", m.protein());
            t.put("carbs", m.carbs());
            t.put("fat", m.fat());
            targets.put(goal, t);
        }
        result.put("targets", targets);
        return JsonUtil.toJson(result);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
