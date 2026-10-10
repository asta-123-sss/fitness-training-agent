package com.fitness.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 创建扫码会话响应。
 */
@Data
@AllArgsConstructor
public class QrCreateResponse {

    private String qrId;

    /** 二维码有效时长（秒）。 */
    private long expireSeconds;
}
