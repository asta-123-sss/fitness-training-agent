package com.fitness.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fitness.agent.util.JsonUtil;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BMI 计算工具：根据身高、体重计算 BMI 并给出分级。
 */
public class BmiCalculatorTool implements AgentTool {

    @Override
    public String name() {
        return "calculate_bmi";
    }

    @Override
    public String description() {
        return "根据身高(厘米)和体重(公斤)计算 BMI 指数，并给出分级（偏瘦/正常/超重/肥胖）。";
    }

    @Override
    public JsonObjectSchema parameters() {
        return JsonObjectSchema.builder()
                .addNumberProperty("heightCm", "身高，单位厘米")
                .addNumberProperty("weightKg", "体重，单位公斤")
                .required("heightCm", "weightKg")
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        JsonNode node = JsonUtil.readTree(argumentsJson);
        double heightCm = node.path("heightCm").asDouble();
        double weightKg = node.path("weightKg").asDouble();

        if (heightCm <= 0 || weightKg <= 0) {
            return "{\"error\":\"身高和体重必须为正数\"}";
        }

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bmi", Math.round(bmi * 100.0) / 100.0);
        result.put("category", category(bmi));
        return JsonUtil.toJson(result);
    }

    private String category(double bmi) {
        if (bmi < 18.5) {
            return "偏瘦";
        }
        if (bmi < 24) {
            return "正常";
        }
        if (bmi < 28) {
            return "超重";
        }
        return "肥胖";
    }
}
