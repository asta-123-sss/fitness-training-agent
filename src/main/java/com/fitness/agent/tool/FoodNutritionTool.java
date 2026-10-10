package com.fitness.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fitness.agent.model.Food;
import com.fitness.agent.service.FoodCatalog;
import com.fitness.agent.util.JsonUtil;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 食物营养查询工具：输入食物名称与重量，返回该重量的热量与三大营养素。
 */
public class FoodNutritionTool implements AgentTool {

    @Override
    public String name() {
        return "food_nutrition";
    }

    @Override
    public String description() {
        return "查询某种食物的营养：输入食物名称和重量（克），返回该重量对应的热量（千卡）、蛋白质、碳水、脂肪（克）。";
    }

    @Override
    public JsonObjectSchema parameters() {
        return JsonObjectSchema.builder()
                .addStringProperty("foodName", "食物名称，如 鸡胸肉、米饭、鸡蛋")
                .addNumberProperty("weightGrams", "重量，单位克")
                .required("foodName", "weightGrams")
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        JsonNode node = JsonUtil.readTree(argumentsJson);
        String foodName = node.path("foodName").asText();
        double weightGrams = node.path("weightGrams").asDouble();

        Optional<Food> food = resolve(foodName);
        if (food.isEmpty()) {
            return "{\"error\":\"未找到该食物，请使用常见食物名称（如 鸡胸肉、米饭、鸡蛋）\"}";
        }
        Food f = food.get();
        double factor = weightGrams / 100.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("foodName", f.getName());
        result.put("weightGrams", weightGrams);
        result.put("calories", round1(f.getCalories() * factor));
        result.put("protein", round1(f.getProtein() * factor));
        result.put("carbs", round1(f.getCarbs() * factor));
        result.put("fat", round1(f.getFat() * factor));
        return JsonUtil.toJson(result);
    }

    private Optional<Food> resolve(String name) {
        Optional<Food> exact = FoodCatalog.findByName(name);
        if (exact.isPresent()) {
            return exact;
        }
        List<Food> candidates = FoodCatalog.search(name);
        return candidates.stream().findFirst();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
