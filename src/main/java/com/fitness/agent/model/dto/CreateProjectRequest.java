package com.fitness.agent.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建项目请求。
 */
@Data
public class CreateProjectRequest {

    @NotBlank(message = "项目名不能为空")
    @Size(max = 50, message = "项目名不能超过 50 字")
    private String name;
}
