package com.fitness.agent.statemachine;

import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.TaskEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 轻量自研任务状态机：维护状态迁移表，校验并执行状态流转。
 *
 * <p>合法迁移：</p>
 * <pre>
 * PENDING_PARSE -> PLANNING / REPLYING / FAILED
 * PLANNING      -> REVIEWING / FAILED
 * REVIEWING     -> PLANNING（重规划）/ COMPLETED / FAILED
 * REPLYING      -> COMPLETED / FAILED
 * </pre>
 */
@Component
public class TaskStateMachine {

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = Map.of(
            TaskStatus.PENDING_PARSE, Set.of(TaskStatus.PLANNING, TaskStatus.REPLYING, TaskStatus.FAILED),
            TaskStatus.PLANNING, Set.of(TaskStatus.REVIEWING, TaskStatus.FAILED),
            TaskStatus.REVIEWING, Set.of(TaskStatus.PLANNING, TaskStatus.COMPLETED, TaskStatus.FAILED),
            TaskStatus.REPLYING, Set.of(TaskStatus.COMPLETED, TaskStatus.FAILED),
            TaskStatus.COMPLETED, Set.of(),
            TaskStatus.FAILED, Set.of()
    );

    /**
     * 校验并执行状态迁移（仅修改内存状态，持久化由调用方负责）。
     */
    public void transition(TaskEntity task, TaskStatus target) {
        TaskStatus current = task.getStatus();
        Set<TaskStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException("非法状态迁移：" + current + " -> " + target);
        }
        task.setStatus(target);
    }
}
