package com.fitness.agent.service;

import com.fitness.agent.model.Conversation;
import com.fitness.agent.model.TaskEntity;
import com.fitness.agent.repository.ConversationRepository;
import com.fitness.agent.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 一次性迁移回填：为所有尚未关联对话的旧 task 各生成一个单轮对话，
 * 保证升级后旧历史仍能在「历史任务」列表中看到。幂等：回填完成后下次启动不再处理。
 */
@Component
public class ConversationBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConversationBackfill.class);

    private final TaskRepository taskRepository;
    private final ConversationRepository conversationRepository;

    public ConversationBackfill(TaskRepository taskRepository, ConversationRepository conversationRepository) {
        this.taskRepository = taskRepository;
        this.conversationRepository = conversationRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<TaskEntity> orphans = taskRepository.findByConversationIdIsNull();
        int migrated = 0;
        for (TaskEntity task : orphans) {
            if (task.getUserId() == null) {
                // 历史匿名任务不进入任何人的列表，跳过。
                continue;
            }
            Conversation conversation = new Conversation();
            conversation.setConversationId(UUID.randomUUID().toString().replace("-", ""));
            conversation.setUserId(task.getUserId());
            conversation.setPinned(false);
            conversation.setArchived(false);
            conversationRepository.save(conversation);

            task.setConversationId(conversation.getConversationId());
            taskRepository.save(task);
            migrated++;
        }
        if (migrated > 0) {
            log.info("已回填 {} 条历史任务为单轮对话", migrated);
        }
    }
}
