package com.fitness.agent.model.dto;

import com.fitness.agent.model.UserProfile;
import lombok.Builder;
import lombok.Data;

/**
 * 训练方案结果响应（创建者可见完整信息）。
 */
@Data
@Builder
public class PlanResultResponse {

    private String sessionId;
    private String status;
    private String statusLabel;
    private String userInput;
    private UserProfile profile;
    private String planMarkdown;
}
