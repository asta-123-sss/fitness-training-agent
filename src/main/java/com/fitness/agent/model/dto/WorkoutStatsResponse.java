package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 训练打卡统计：累计次数、本周次数、各部位次数。
 */
@Data
@Builder
public class WorkoutStatsResponse {

    private long totalCount;
    private long weekCount;
    private Map<String, Integer> byBodyPart;
}
