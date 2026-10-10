package com.fitness.agent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新增训练打卡请求。
 */
@Data
public class WorkoutLogRequest {

    /** 训练日期（可空，默认今天）。 */
    private LocalDate workoutDate;

    @NotBlank(message = "请填写训练部位")
    private String bodyPart;

    @NotBlank(message = "请填写动作名称")
    private String exerciseName;

    /** 重量（公斤，可空）。 */
    private Double weightKg;

    /** 组数（可空）。 */
    private Integer sets;

    /** 次数，如 "12" 或 "8-12"（可空）。 */
    private String reps;

    /** 训练感受 / 备注（可空）。 */
    private String feeling;
}
