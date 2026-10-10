package com.fitness.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 健身动作知识库中的单条动作记录。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {

    /** 动作唯一标识。 */
    private String id;

    /** 动作名称。 */
    private String name;

    /** 主要训练部位：胸 / 背 / 腿 / 肩 / 核心 / 全身心肺。 */
    private String category;

    /** 难度：1 简单 / 2 中等 / 3 较难。 */
    private int difficulty;

    /** 需要的器械（空表示徒手）。 */
    private List<String> equipment = new ArrayList<>();

    /** 禁忌人群/伤病，如 ["膝伤","腰伤"]。 */
    private List<String> contraindications = new ArrayList<>();

    /** 动作要点描述。 */
    private String description;
}
