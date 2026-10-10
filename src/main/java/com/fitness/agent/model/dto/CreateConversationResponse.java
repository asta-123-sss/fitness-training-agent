package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 新建对话响应。
 */
@Data
@Builder
public class CreateConversationResponse {

    private String conversationId;
}
