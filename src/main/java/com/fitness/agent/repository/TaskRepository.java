package com.fitness.agent.repository;

import com.fitness.agent.model.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 任务持久化仓库。
 */
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findBySessionId(String sessionId);

    List<TaskEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<TaskEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<TaskEntity> findByConversationIdIsNull();
}
