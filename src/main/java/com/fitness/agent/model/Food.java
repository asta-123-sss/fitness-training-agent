package com.fitness.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 食物库中的单条食物记录（每 100 克营养数据）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Food {

    /** 食物名称。 */
    private String name;

    /** 分类，如 主食 / 肉蛋 / 奶类 / 豆制品 / 蔬菜 / 水果 / 坚果油脂。 */
    private String category;

    /** 每 100 克热量（千卡）。 */
    private double calories;

    /** 每 100 克蛋白质（克）。 */
    private double protein;

    /** 每 100 克碳水（克）。 */
    private double carbs;

    /** 每 100 克脂肪（克）。 */
    private double fat;
}
