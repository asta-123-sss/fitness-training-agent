package com.fitness.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录 / 注册响应：token + 用户信息。
 */
@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UserVO user;
}
