package com.fitness.agent.model.dto;

import lombok.Data;

/**
 * 移动任务到项目请求；projectId 为 null 表示移出项目。
 */
@Data
public class MoveRequest {
    private Long projectId;
}
