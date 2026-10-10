package com.fitness.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * IntentExtractor 从用户自然语言中提取的结构化健身信息。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile {

    /** 身高（厘米）。 */
    private double heightCm;

    /** 体重（公斤）。 */
    private double weightKg;

    /** 性别：男 / 女。 */
    private String gender;

    /** 年龄。 */
    private Integer age;

    /** 伤病或禁忌部位，如 ["膝伤"]。 */
    private List<String> injuries = new ArrayList<>();

    /** 可用器械，如 ["哑铃","弹力带"]。 */
    private List<String> equipment = new ArrayList<>();

    /** 用户想重点锻炼的部位/肌群，如 ["胸部","手臂"]。 */
    private List<String> targetMuscles = new ArrayList<>();

    /** 每周可训练天数。 */
    private int daysPerWeek;

    /** 健身目标：增肌 / 减脂 / 维持 / 塑形。 */
    private String goal;

    /** 单次训练时长限制（分钟）。 */
    private int durationMinutes;
}
