package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的任务列表摘要。
 */
@Data
@Builder
public class TaskSummaryResponse {

    private String sessionId;
    private String status;
    private String statusLabel;
    /** 用户原始输入（用于列表预览，前端截断展示）。 */
    private String userInput;
    /** 自定义标题（重命名），可能为空。 */
    private String title;
    /** 是否置顶。 */
    private Boolean pinned;
    /** 所属项目名（可能为空）。 */
    private String projectName;
    private LocalDateTime createdAt;
}
