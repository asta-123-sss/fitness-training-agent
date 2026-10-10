package com.fitness.agent.repository;

import com.fitness.agent.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 项目持久化仓库。
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<Project> findByUserIdAndName(Long userId, String name);
}
