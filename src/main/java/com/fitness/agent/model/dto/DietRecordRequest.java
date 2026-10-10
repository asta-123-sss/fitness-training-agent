package com.fitness.agent.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

/**
 * 添加饮食记录请求。
 */
@Data
public class DietRecordRequest {

    /** 记录日期（可空，默认今天）。 */
    private LocalDate recordDate;

    /** 餐次：早餐 / 午餐 / 晚餐 / 加餐（可空）。 */
    private String mealType;

    @NotBlank(message = "请选择食物")
    private String foodName;

    @NotNull(message = "请填写重量")
    @Positive(message = "重量必须为正数")
    private Integer weightGrams;
}
