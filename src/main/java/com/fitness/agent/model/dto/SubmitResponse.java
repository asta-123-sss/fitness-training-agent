package com.fitness.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 提交任务响应，立即返回 sessionId，流水线在后台异步执行。
 */
@Data
@AllArgsConstructor
public class SubmitResponse {

    private String sessionId;
    private String status;
    private String statusLabel;
}
