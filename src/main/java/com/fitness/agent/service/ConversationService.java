package com.fitness.agent.service;

import com.fitness.agent.auth.UserContext;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.Conversation;
import com.fitness.agent.model.Project;
import com.fitness.agent.model.TaskEntity;
import com.fitness.agent.model.dto.ConversationDetailResponse;
import com.fitness.agent.model.dto.ConversationMessageResponse;
import com.fitness.agent.model.dto.ConversationResponse;
import com.fitness.agent.model.dto.ConversationShareResponse;
import com.fitness.agent.repository.ConversationRepository;
import com.fitness.agent.repository.ProjectRepository;
import com.fitness.agent.repository.TaskRepository;
import com.fitness.agent.statemachine.TaskStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 对话服务：以「对话」为历史任务单位，聚合同一页面内的多轮问答。
 * 置顶 / 重命名 / 归档 / 删除 / 项目 等元数据操作都在对话维度上进行。
 */
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               TaskRepository taskRepository,
                               ProjectRepository projectRepository) {
        this.conversationRepository = conversationRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    /** 新建对话，返回 conversationId。 */
    public String create() {
        Long userId = requireUser();
        Conversation conversation = new Conversation();
        conversation.setConversationId(UUID.randomUUID().toString().replace("-", ""));
        conversation.setUserId(userId);
        conversation.setPinned(false);
        conversation.setArchived(false);
        conversationRepository.save(conversation);
        return conversation.getConversationId();
    }

    /** 提交时复用：conversationId 为空则新建，否则校验归属后返回。 */
    public String getOrCreate(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return create();
        }
        return getOwnedConversation(conversationId).getConversationId();
    }

    /** 对话列表：过滤归档，置顶优先、按时间倒序。 */
    public List<ConversationResponse> list() {
        Long userId = UserContext.get();
        if (userId == null) {
            return List.of();
        }
        Map<Long, String> projectNames = projectRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));

        Map<String, List<TaskEntity>> tasksByConversation = taskRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(t -> t.getConversationId() != null)
                .collect(Collectors.groupingBy(TaskEntity::getConversationId));

        Comparator<Conversation> pinnedFirst = Comparator.comparing(
                (Conversation c) -> Boolean.TRUE.equals(c.getPinned()));
        Comparator<Conversation> byTimeDesc = Comparator.comparing(
                (Conversation c) -> c.getPinnedAt() != null ? c.getPinnedAt() : c.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder()));

        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(c -> !Boolean.TRUE.equals(c.getArchived()))
                .filter(c -> tasksByConversation.containsKey(c.getConversationId()))
                .sorted(pinnedFirst.reversed().thenComparing(byTimeDesc))
                .map(c -> toResponse(c, projectNames, tasksByConversation))
                .toList();
    }

    /** 对话详情：标题 + 全部轮次（按时间升序）。 */
    public ConversationDetailResponse detail(String conversationId) {
        Conversation conversation = getOwnedConversation(conversationId);
        List<ConversationMessageResponse> messages = taskRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessage)
                .toList();
        return ConversationDetailResponse.builder()
                .conversationId(conversation.getConversationId())
                .title(conversation.getTitle())
                .messages(messages)
                .build();
    }

    /** 分享只读：聚合所有已完成轮次的方案，不暴露用户原始输入。 */
    public ConversationShareResponse share(String conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new BusinessException("未找到对应的对话"));
        StringBuilder sb = new StringBuilder();
        for (TaskEntity task : taskRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)) {
            if (task.getStatus() == TaskStatus.COMPLETED && task.getPlanMarkdown() != null) {
                if (sb.length() > 0) {
                    sb.append("\n\n---\n\n");
                }
                sb.append(task.getPlanMarkdown());
            }
        }
        return ConversationShareResponse.builder()
                .conversationId(conversation.getConversationId())
                .title(conversation.getTitle())
                .planMarkdown(sb.toString())
                .build();
    }

    /** 置顶 / 取消置顶。 */
    public void pin(String conversationId, boolean pinned) {
        Conversation conversation = getOwnedConversation(conversationId);
        conversation.setPinned(pinned);
        conversation.setPinnedAt(pinned ? LocalDateTime.now() : null);
        conversationRepository.save(conversation);
    }

    /** 重命名（自定义标题）。 */
    public void rename(String conversationId, String title) {
        Conversation conversation = getOwnedConversation(conversationId);
        conversation.setTitle(title.trim());
        conversationRepository.save(conversation);
    }

    /** 归档（从默认列表隐藏）。 */
    public void archive(String conversationId) {
        Conversation conversation = getOwnedConversation(conversationId);
        conversation.setArchived(true);
        conversationRepository.save(conversation);
    }

    /** 删除对话及其下全部轮次。 */
    public void delete(String conversationId) {
        Conversation conversation = getOwnedConversation(conversationId);
        taskRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .forEach(taskRepository::delete);
        conversationRepository.delete(conversation);
    }

    /** 移动到项目（projectId 传 null 表示移出项目）。 */
    public void move(String conversationId, Long projectId) {
        Conversation conversation = getOwnedConversation(conversationId);
        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new BusinessException("项目不存在"));
            Long userId = UserContext.get();
            if (userId == null || !userId.equals(project.getUserId())) {
                throw new BusinessException("无权访问该项目");
            }
        }
        conversation.setProjectId(projectId);
        conversationRepository.save(conversation);
    }

    private Long requireUser() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }

    /** 校验对话归属，非本人不可访问。 */
    private Conversation getOwnedConversation(String conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new BusinessException("未找到对应的对话"));
        Long userId = UserContext.get();
        if (userId == null || !userId.equals(conversation.getUserId())) {
            throw new BusinessException("无权访问该对话");
        }
        return conversation;
    }

    private ConversationMessageResponse toMessage(TaskEntity task) {
        return ConversationMessageResponse.builder()
                .sessionId(task.getSessionId())
                .userInput(task.getUserInput())
                .status(task.getStatus().name())
                .statusLabel(task.getStatus().getLabel())
                .planMarkdown(task.getPlanMarkdown())
                .build();
    }

    private ConversationResponse toResponse(Conversation conversation,
                                            Map<Long, String> projectNames,
                                            Map<String, List<TaskEntity>> tasksByConversation) {
        List<TaskEntity> turns = tasksByConversation.getOrDefault(conversation.getConversationId(), List.of())
                .stream()
                .sorted(Comparator.comparing(TaskEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        String userInput = turns.isEmpty() ? null : turns.get(0).getUserInput();
        TaskEntity latest = turns.isEmpty() ? null : turns.get(turns.size() - 1);

        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .title(conversation.getTitle())
                .pinned(conversation.getPinned())
                .projectName(conversation.getProjectId() != null ? projectNames.get(conversation.getProjectId()) : null)
                .status(latest != null ? latest.getStatus().name() : null)
                .statusLabel(latest != null ? latest.getStatus().getLabel() : null)
                .userInput(userInput)
                .messageCount(turns.size())
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
