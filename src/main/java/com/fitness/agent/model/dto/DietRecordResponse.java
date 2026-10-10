package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食记录响应。
 */
@Data
@Builder
public class DietRecordResponse {

    private Long id;
    private LocalDate recordDate;
    private String mealType;
    private String foodName;
    private Integer weightGrams;
    private Double calories;
    private Double protein;
    private Double carbs;
    private Double fat;
    private LocalDateTime createdAt;
}
