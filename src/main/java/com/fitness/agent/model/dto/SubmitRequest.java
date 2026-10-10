package com.fitness.agent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交任务请求。
 */
@Data
public class SubmitRequest {

    @NotBlank(message = "请填写您的身体情况与健身目标")
    private String message;

    /** 所属对话 ID（可选）；为空时后端自动新建对话。 */
    private String conversationId;
}
