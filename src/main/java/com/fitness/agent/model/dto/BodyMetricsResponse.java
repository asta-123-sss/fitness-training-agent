package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 身体指标计算结果（含三档目标推荐）。
 */
@Data
@Builder
public class BodyMetricsResponse {

    private String gender;
    private Integer age;
    private Double heightCm;
    private Double weightKg;
    private String activityLevel;

    private Double bmi;
    private String bmiCategory;
    private Integer bmr;
    private Integer tdee;

    /** 已保存的目标（可空）。 */
    private String goal;
    /** 已保存目标对应的热量（可空）。 */
    private Integer targetCalories;

    /** 三档目标（减脂/维持/增肌）的推荐热量与营养素。 */
    private List<TargetNutrition> targets;
}
