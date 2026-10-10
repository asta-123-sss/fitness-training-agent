package com.fitness.agent.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 项目摘要。
 */
@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
}
