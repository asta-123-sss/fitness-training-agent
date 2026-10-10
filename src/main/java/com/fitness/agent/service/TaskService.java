package com.fitness.agent.service;

import com.fitness.agent.auth.UserContext;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.Project;
import com.fitness.agent.model.TaskEntity;
import com.fitness.agent.model.UserProfile;
import com.fitness.agent.model.dto.PlanResultResponse;
import com.fitness.agent.model.dto.ShareResponse;
import com.fitness.agent.model.dto.SubmitRequest;
import com.fitness.agent.model.dto.SubmitResponse;
import com.fitness.agent.model.dto.TaskProgressResponse;
import com.fitness.agent.model.dto.TaskSummaryResponse;
import com.fitness.agent.repository.ProjectRepository;
import com.fitness.agent.repository.TaskRepository;
import com.fitness.agent.statemachine.TaskStatus;
import com.fitness.agent.util.JsonUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 任务服务：提交、进度查询、结果查询、分享只读。
 */
@Service
public class TaskService {

    private final TaskRepository repository;
    private final ProjectRepository projectRepository;
    private final AgentOrchestrator orchestrator;
    private final ConversationService conversationService;

    public TaskService(TaskRepository repository, ProjectRepository projectRepository,
                       AgentOrchestrator orchestrator, ConversationService conversationService) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.orchestrator = orchestrator;
        this.conversationService = conversationService;
    }

    /** 提交任务：归入对话（无则新建），创建本轮记录并异步触发 Agent 流水线。 */
    public SubmitResponse submit(SubmitRequest request) {
        String conversationId = conversationService.getOrCreate(request.getConversationId());

        TaskEntity task = new TaskEntity();
        task.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        task.setUserInput(request.getMessage());
        task.setUserId(UserContext.get());
        task.setConversationId(conversationId);
        task.setStatus(TaskStatus.PENDING_PARSE);
        repository.save(task);

        orchestrator.execute(task.getSessionId());

        return new SubmitResponse(task.getSessionId(), task.getStatus().name(), task.getStatus().getLabel());
    }

    /** 我的任务列表：过滤归档，置顶优先、按时间倒序。 */
    public List<TaskSummaryResponse> list() {
        Long userId = UserContext.get();
        if (userId == null) {
            return List.of();
        }
        Map<Long, String> projectNames = projectRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));

        Comparator<TaskEntity> pinnedFirst = Comparator.comparing(
                (TaskEntity t) -> Boolean.TRUE.equals(t.getPinned()));
        Comparator<TaskEntity> byTimeDesc = Comparator.comparing(
                (TaskEntity t) -> t.getPinnedAt() != null ? t.getPinnedAt() : t.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder()));

        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getArchived()))
                .sorted(pinnedFirst.reversed().thenComparing(byTimeDesc))
                .map(t -> toSummary(t, projectNames))
                .toList();
    }

    /** 查询任务进度。 */
    public TaskProgressResponse progress(String sessionId) {
        TaskEntity task = getOwnedTask(sessionId);
        return TaskProgressResponse.builder()
                .sessionId(sessionId)
                .status(task.getStatus().name())
                .statusLabel(task.getStatus().getLabel())
                .errorMsg(task.getErrorMsg())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    /** 获取完整结果（创建者视角）。 */
    public PlanResultResponse result(String sessionId) {
        TaskEntity task = getOwnedTask(sessionId);
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new BusinessException("任务尚未完成，当前状态：" + task.getStatus().getLabel());
        }
        UserProfile profile = task.getProfileJson() == null
                ? null
                : JsonUtil.readValue(task.getProfileJson(), UserProfile.class);
        return PlanResultResponse.builder()
                .sessionId(sessionId)
                .status(task.getStatus().name())
                .statusLabel(task.getStatus().getLabel())
                .userInput(task.getUserInput())
                .profile(profile)
                .planMarkdown(task.getPlanMarkdown())
                .build();
    }

    /** 分享只读：仅暴露最终方案，不暴露用户原始输入与个人信息。 */
    public ShareResponse share(String sessionId) {
        TaskEntity task = getTask(sessionId);
        return ShareResponse.builder()
                .sessionId(sessionId)
                .status(task.getStatus().name())
                .statusLabel(task.getStatus().getLabel())
                .planMarkdown(task.getStatus() == TaskStatus.COMPLETED ? task.getPlanMarkdown() : null)
                .errorMsg(task.getErrorMsg())
                .build();
    }

    /** 置顶 / 取消置顶。 */
    public void pin(String sessionId, boolean pinned) {
        TaskEntity task = getOwnedTask(sessionId);
        task.setPinned(pinned);
        task.setPinnedAt(pinned ? LocalDateTime.now() : null);
        repository.save(task);
    }

    /** 重命名（自定义标题）。 */
    public void rename(String sessionId, String title) {
        TaskEntity task = getOwnedTask(sessionId);
        task.setTitle(title.trim());
        repository.save(task);
    }

    /** 归档（从默认列表隐藏）。 */
    public void archive(String sessionId) {
        TaskEntity task = getOwnedTask(sessionId);
        task.setArchived(true);
        repository.save(task);
    }

    /** 删除任务。 */
    public void delete(String sessionId) {
        repository.delete(getOwnedTask(sessionId));
    }

    /** 移动到项目（projectId 传 null 表示移出项目）。 */
    public void move(String sessionId, Long projectId) {
        TaskEntity task = getOwnedTask(sessionId);
        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new BusinessException("项目不存在"));
            Long userId = UserContext.get();
            if (userId == null || !userId.equals(project.getUserId())) {
                throw new BusinessException("无权访问该项目");
            }
        }
        task.setProjectId(projectId);
        repository.save(task);
    }

    private TaskEntity getTask(String sessionId) {
        return repository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("未找到对应的任务，sessionId：" + sessionId));
    }

    /** 校验任务归属，非本人（或历史匿名任务）不可访问。 */
    private TaskEntity getOwnedTask(String sessionId) {
        TaskEntity task = getTask(sessionId);
        Long userId = UserContext.get();
        if (userId == null || !userId.equals(task.getUserId())) {
            throw new BusinessException("无权访问该任务");
        }
        return task;
    }

    private TaskSummaryResponse toSummary(TaskEntity task, Map<Long, String> projectNames) {
        return TaskSummaryResponse.builder()
                .sessionId(task.getSessionId())
                .status(task.getStatus().name())
                .statusLabel(task.getStatus().getLabel())
                .userInput(task.getUserInput())
                .title(task.getTitle())
                .pinned(task.getPinned())
                .projectName(task.getProjectId() != null ? projectNames.get(task.getProjectId()) : null)
                .createdAt(task.getCreatedAt())
                .build();
    }
}
