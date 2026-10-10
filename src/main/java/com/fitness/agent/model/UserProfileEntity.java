package com.fitness.agent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户身体指标档案（每个用户一行）：保存身体指标计算器结果，供 AI 对话复用。
 */
@Data
@Entity
@Table(name = "user_profile")
public class UserProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户 ID（唯一，一个用户一份档案）。 */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** 性别：男 / 女。 */
    private String gender;

    /** 年龄。 */
    private Integer age;

    /** 身高（厘米）。 */
    private Double heightCm;

    /** 体重（公斤）。 */
    private Double weightKg;

    /** 日常活动量：久坐 / 轻度 / 中度 / 高度 / 极高。 */
    private String activityLevel;

    /** 身体质量指数。 */
    private Double bmi;

    /** 基础代谢 BMR（千卡）。 */
    private Double bmr;

    /** 每日总消耗 TDEE（千卡）。 */
    private Double tdee;

    /** 健身目标：减脂 / 维持 / 增肌（可空）。 */
    private String goal;

    /** 目标热量（千卡，可空；为空时饮食工具回退用 TDEE 作对比）。 */
    private Integer targetCalories;

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
