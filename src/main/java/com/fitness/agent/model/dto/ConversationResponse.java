package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话列表摘要（历史任务列表的单位）。
 */
@Data
@Builder
public class ConversationResponse {

    private String conversationId;
    /** 自定义标题（重命名），可能为空。 */
    private String title;
    /** 是否置顶。 */
    private Boolean pinned;
    /** 所属项目名（可能为空）。 */
    private String projectName;
    /** 最新一轮状态。 */
    private String status;
    private String statusLabel;
    /** 首条提问（列表预览，前端截断展示）。 */
    private String userInput;
    /** 轮次数量。 */
    private int messageCount;
    private LocalDateTime createdAt;
}
