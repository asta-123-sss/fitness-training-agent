package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日饮食汇总：全天合计 + 与目标热量对比。
 */
@Data
@Builder
public class DietSummaryResponse {

    private LocalDate date;
    private Double totalCalories;
    private Double totalProtein;
    private Double totalCarbs;
    private Double totalFat;

    /** 目标热量（千卡，可空；为空时前端不显示对比）。 */
    private Integer targetCalories;

    private List<DietRecordResponse> records;
}
