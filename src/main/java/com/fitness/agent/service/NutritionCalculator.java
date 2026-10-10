package com.fitness.agent.service;

/**
 * 营养/代谢计算工具函数（无状态）：BMI、BMR（Mifflin-St Jeor）、TDEE、目标热量与三大营养素。
 * 供 REST 服务与 Agent 工具共用。
 */
public final class NutritionCalculator {

    private NutritionCalculator() {
    }

    /** 三大营养素（克）。 */
    public record Macros(double protein, double carbs, double fat) {
    }

    /** BMI = 体重(kg) / 身高(m)^2。 */
    public static double bmi(double heightCm, double weightKg) {
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    /** BMI 分级（中国标准）。 */
    public static String bmiCategory(double bmi) {
        if (bmi < 18.5) {
            return "偏瘦";
        }
        if (bmi < 24) {
            return "正常";
        }
        if (bmi < 28) {
            return "超重";
        }
        return "肥胖";
    }

    /** Mifflin-St Jeor 基础代谢公式。 */
    public static double bmr(String gender, int age, double heightCm, double weightKg) {
        if ("女".equals(gender)) {
            return 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
        }
        return 10 * weightKg + 6.25 * heightCm - 5 * age + 5;
    }

    /** 每日总消耗 = BMR × 活动系数。 */
    public static double tdee(double bmr, String activityLevel) {
        return bmr * activityFactor(activityLevel);
    }

    /** 目标热量：减脂 -400、增肌 +300、维持不变。 */
    public static int targetCalories(double tdee, String goal) {
        return switch (goal == null ? "" : goal) {
            case "减脂" -> (int) Math.round(tdee - 400);
            case "增肌" -> (int) Math.round(tdee + 300);
            default -> (int) Math.round(tdee);
        };
    }

    /**
     * 三大营养素：蛋白质按目标取 g/kg，脂肪占目标热量 25%，碳水补足剩余热量。
     */
    public static Macros macros(String goal, double weightKg, int targetCalories) {
        double proteinPerKg = switch (goal == null ? "" : goal) {
            case "减脂" -> 2.0;
            case "增肌" -> 1.8;
            default -> 1.6;
        };
        double protein = proteinPerKg * weightKg;
        double fat = 0.25 * targetCalories / 9.0;
        double carbs = (targetCalories - protein * 4 - fat * 9) / 4.0;
        if (carbs < 0) {
            carbs = 0;
        }
        return new Macros(round1(protein), round1(carbs), round1(fat));
    }

    /** 活动量 → 活动系数（默认轻度 1.375）。 */
    public static double activityFactor(String activityLevel) {
        if (activityLevel == null) {
            return 1.375;
        }
        if (activityLevel.contains("久坐")) {
            return 1.2;
        }
        if (activityLevel.contains("轻度")) {
            return 1.375;
        }
        if (activityLevel.contains("中度")) {
            return 1.55;
        }
        if (activityLevel.contains("高度")) {
            return 1.725;
        }
        if (activityLevel.contains("极高")) {
            return 1.9;
        }
        return 1.375;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
