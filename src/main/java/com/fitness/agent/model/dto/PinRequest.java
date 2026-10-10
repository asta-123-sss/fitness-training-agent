package com.fitness.agent.model.dto;

import lombok.Data;

/**
 * 置顶 / 取消置顶请求。
 */
@Data
public class PinRequest {
    private boolean pinned;
}
