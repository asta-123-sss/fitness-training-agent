package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 分享只读响应：不含用户原始输入与个人信息，仅暴露最终方案。
 */
@Data
@Builder
public class ShareResponse {

    private String sessionId;
    private String status;
    private String statusLabel;
    private String planMarkdown;
    private String errorMsg;
}
