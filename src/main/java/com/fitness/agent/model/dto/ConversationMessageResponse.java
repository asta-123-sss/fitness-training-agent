package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 对话内的一轮问答。
 */
@Data
@Builder
public class ConversationMessageResponse {

    private String sessionId;
    private String userInput;
    private String status;
    private String statusLabel;
    private String planMarkdown;
}
