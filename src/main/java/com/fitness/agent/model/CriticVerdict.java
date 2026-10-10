package com.fitness.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Critic 评审 Agent 的输出结果。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CriticVerdict {

    /** 是否通过评审。 */
    private boolean pass;

    /** 发现的问题列表。 */
    private List<String> issues = new ArrayList<>();

    /** 改进建议列表。 */
    private List<String> suggestions = new ArrayList<>();
}
