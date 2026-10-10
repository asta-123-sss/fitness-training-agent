package com.fitness.agent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手机端确认扫码登录请求。
 */
@Data
public class QrConfirmRequest {

    @NotBlank(message = "qrId 不能为空")
    private String qrId;
}
