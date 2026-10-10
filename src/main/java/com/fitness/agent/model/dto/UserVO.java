package com.fitness.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息视图（不暴露密码哈希）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String phone;
    private String nickname;
}
