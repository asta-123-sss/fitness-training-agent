package com.fitness.agent.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 身体指标计算 / 保存档案请求。
 */
@Data
public class BodyMetricsRequest {

    @NotBlank(message = "请选择性别")
    private String gender;

    @NotNull(message = "请填写年龄")
    private Integer age;

    @NotNull(message = "请填写身高")
    private Double heightCm;

    @NotNull(message = "请填写体重")
    private Double weightKg;

    /** 日常活动量：久坐 / 轻度 / 中度 / 高度 / 极高（可空，默认轻度）。 */
    private String activityLevel;

    /** 健身目标：减脂 / 维持 / 增肌（可空，默认维持）。 */
    private String goal;
}
