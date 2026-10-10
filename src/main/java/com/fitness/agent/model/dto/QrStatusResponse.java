package com.fitness.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 扫码会话状态响应。
 */
@Data
@AllArgsConstructor
public class QrStatusResponse {

    /** PENDING / CONFIRMED / EXPIRED。 */
    private String status;

    /** 已确认时返回给电脑端的登录 token（仅首次领取有效）。 */
    private String token;

    private UserVO user;
}
