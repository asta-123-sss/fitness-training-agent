package com.fitness.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fitness.agent.model.Food;
import com.fitness.agent.service.FoodCatalog;
import com.fitness.agent.util.JsonUtil;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 饮食统计工具：输入多份食物（名称 + 重量），返回合计热量与三大营养素。
 */
public class DietSummaryTool implements AgentTool {

    @Override
    public String name() {
        return "calculate_diet_total";
    }

    @Override
    public String description() {
        return "计算多份食物的合计热量与营养：输入食物列表（每项含 name 食物名称、weightGrams 重量克），"
                + "返回合计的热量（千卡）、蛋白质、碳水、脂肪（克）。";
    }

    @Override
    public JsonObjectSchema parameters() {
        JsonObjectSchema item = JsonObjectSchema.builder()
                .addStringProperty("name", "食物名称")
                .addNumberProperty("weightGrams", "重量，单位克")
                .required("name", "weightGrams")
                .build();
        return JsonObjectSchema.builder()
                .addProperty("foods", JsonArraySchema.builder()
                        .items(item)
                        .description("食物列表")
                        .build())
                .required("foods")
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        JsonNode node = JsonUtil.readTree(argumentsJson);
        JsonNode foods = node.path("foods");

        double calories = 0, protein = 0, carbs = 0, fat = 0;
        int count = 0;
        for (JsonNode item : foods) {
            String name = item.path("name").asText();
            double weight = item.path("weightGrams").asDouble();
            Optional<Food> food = resolve(name);
            if (food.isEmpty()) {
                continue;
            }
            Food f = food.get();
            double factor = weight / 100.0;
            calories += f.getCalories() * factor;
            protein += f.getProtein() * factor;
            carbs += f.getCarbs() * factor;
            fat += f.getFat() * factor;
            count++;
        }

        if (count == 0) {
            return "{\"error\":\"没有识别到可用的食物，请使用常见食物名称\"}";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemCount", count);
        result.put("calories", round1(calories));
        result.put("protein", round1(protein));
        result.put("carbs", round1(carbs));
        result.put("fat", round1(fat));
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
