package com.fitness.agent.service;

import com.fitness.agent.auth.UserContext;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.Project;
import com.fitness.agent.model.dto.CreateProjectRequest;
import com.fitness.agent.model.dto.ProjectResponse;
import com.fitness.agent.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 项目服务：对话分组，支持列表与新建。
 */
@Service
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    /** 当前用户的项目列表。 */
    public List<ProjectResponse> list() {
        Long userId = UserContext.get();
        if (userId == null) {
            return List.of();
        }
        return repository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(p -> ProjectResponse.builder().id(p.getId()).name(p.getName()).build())
                .toList();
    }

    /** 新建项目；同名项目直接复用，避免重复。 */
    public ProjectResponse create(CreateProjectRequest request) {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        String name = request.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("项目名不能为空");
        }
        Optional<Project> existing = repository.findByUserIdAndName(userId, name);
        if (existing.isPresent()) {
            Project p = existing.get();
            return ProjectResponse.builder().id(p.getId()).name(p.getName()).build();
        }
        Project project = new Project();
        project.setUserId(userId);
        project.setName(name);
        repository.save(project);
        return ProjectResponse.builder().id(project.getId()).name(project.getName()).build();
    }
}
