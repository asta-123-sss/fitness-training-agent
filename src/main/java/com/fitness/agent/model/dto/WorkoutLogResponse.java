package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 训练打卡记录响应。
 */
@Data
@Builder
public class WorkoutLogResponse {

    private Long id;
    private LocalDate workoutDate;
    private String bodyPart;
    private String exerciseName;
    private Double weightKg;
    private Integer sets;
    private String reps;
    private String feeling;
    private LocalDateTime createdAt;
}
