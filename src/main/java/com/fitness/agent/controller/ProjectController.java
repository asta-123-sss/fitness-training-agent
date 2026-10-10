package com.fitness.agent.controller;

import com.fitness.agent.model.dto.CreateProjectRequest;
import com.fitness.agent.model.dto.ProjectResponse;
import com.fitness.agent.model.dto.Result;
import com.fitness.agent.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目 REST 接口：列表 / 新建。
 */
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/list")
    public Result<List<ProjectResponse>> list() {
        return Result.success(projectService.list());
    }

    @PostMapping
    public Result<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return Result.success(projectService.create(request));
    }
}
