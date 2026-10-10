package com.fitness.agent.repository;

import com.fitness.agent.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 对话持久化仓库。
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Conversation> findByConversationId(String conversationId);
}
