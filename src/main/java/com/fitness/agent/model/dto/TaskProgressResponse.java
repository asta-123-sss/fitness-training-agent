package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务进度查询响应。
 */
@Data
@Builder
public class TaskProgressResponse {

    private String sessionId;
    private String status;
    private String statusLabel;
    private String errorMsg;
    private LocalDateTime updatedAt;
}
