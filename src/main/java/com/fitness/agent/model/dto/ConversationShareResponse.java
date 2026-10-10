package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 对话分享只读响应：聚合该对话所有已完成轮次的方案。
 */
@Data
@Builder
public class ConversationShareResponse {

    private String conversationId;
    private String title;
    private String planMarkdown;
}
