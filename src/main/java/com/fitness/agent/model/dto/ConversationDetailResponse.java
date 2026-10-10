package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 对话详情：标题 + 全部轮次。
 */
@Data
@Builder
public class ConversationDetailResponse {

    private String conversationId;
    private String title;
    private List<ConversationMessageResponse> messages;
}
