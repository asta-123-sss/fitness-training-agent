package com.fitness.agent.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 训练打卡日志：一条训练记录。
 */
@Data
@Entity
@Table(name = "workout_log")
public class WorkoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户 ID。 */
    private Long userId;

    /** 训练日期。 */
    private LocalDate workoutDate;

    /** 训练部位，如 胸 / 背 / 腿 / 肩 / 核心。 */
    private String bodyPart;

    /** 动作名称。 */
    private String exerciseName;

    /** 重量（公斤，可空）。 */
    private Double weightKg;

    /** 组数（可空）。 */
    private Integer sets;

    /** 次数，如 "12" 或 "8-12"（可空）。 */
    private String reps;

    /** 训练感受 / 备注（可空）。 */
    private String feeling;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
