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
 * 每日饮食记录：一条食物记录，落库时由食物库按重量算出热量与三大营养素并冗余存储，便于汇总。
 */
@Data
@Entity
@Table(name = "diet_record")
public class DietRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户 ID。 */
    private Long userId;

    /** 记录日期。 */
    private LocalDate recordDate;

    /** 餐次：早餐 / 午餐 / 晚餐 / 加餐（可空）。 */
    private String mealType;

    /** 食物名称（来自食物库）。 */
    private String foodName;

    /** 重量（克）。 */
    private Integer weightGrams;

    /** 热量（千卡）。 */
    private Double calories;

    /** 蛋白质（克）。 */
    private Double protein;

    /** 碳水（克）。 */
    private Double carbs;

    /** 脂肪（克）。 */
    private Double fat;

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
