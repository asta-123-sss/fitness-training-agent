package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 单个目标对应的推荐热量与三大营养素。
 */
@Data
@Builder
public class TargetNutrition {

    private String goal;
    private Integer calories;
    private Double protein;
    private Double carbs;
    private Double fat;
}
