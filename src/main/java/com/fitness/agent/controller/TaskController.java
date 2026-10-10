package com.fitness.agent.controller;

import com.fitness.agent.model.dto.MoveRequest;
import com.fitness.agent.model.dto.PinRequest;
import com.fitness.agent.model.dto.PlanResultResponse;
import com.fitness.agent.model.dto.RenameRequest;
import com.fitness.agent.model.dto.Result;
import com.fitness.agent.model.dto.ShareResponse;
import com.fitness.agent.model.dto.SubmitRequest;
import com.fitness.agent.model.dto.SubmitResponse;
import com.fitness.agent.model.dto.TaskProgressResponse;
import com.fitness.agent.model.dto.TaskSummaryResponse;
import com.fitness.agent.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 任务 REST 接口：提交 / 进度 / 结果 / 分享只读。
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** 提交健身需求。 */
    @PostMapping("/submit")
    public Result<SubmitResponse> submit(@Valid @RequestBody SubmitRequest request) {
        return Result.success(taskService.submit(request));
    }

    /** 我的任务列表。 */
    @GetMapping("/list")
    public Result<List<TaskSummaryResponse>> list() {
        return Result.success(taskService.list());
    }

    /** 查询任务进度（前端轮询）。 */
    @GetMapping("/{sessionId}")
    public Result<TaskProgressResponse> progress(@PathVariable String sessionId) {
        return Result.success(taskService.progress(sessionId));
    }

    /** 获取训练结果。 */
    @GetMapping("/{sessionId}/result")
    public Result<PlanResultResponse> result(@PathVariable String sessionId) {
        return Result.success(taskService.result(sessionId));
    }

    /** 分享只读。 */
    @GetMapping("/{sessionId}/share")
    public Result<ShareResponse> share(@PathVariable String sessionId) {
        return Result.success(taskService.share(sessionId));
    }

    /** 置顶 / 取消置顶。 */
    @PostMapping("/{sessionId}/pin")
    public Result<Void> pin(@PathVariable String sessionId, @RequestBody PinRequest request) {
        taskService.pin(sessionId, request.isPinned());
        return Result.success(null);
    }

    /** 重命名。 */
    @PostMapping("/{sessionId}/rename")
    public Result<Void> rename(@PathVariable String sessionId, @Valid @RequestBody RenameRequest request) {
        taskService.rename(sessionId, request.getTitle());
        return Result.success(null);
    }

    /** 归档。 */
    @PostMapping("/{sessionId}/archive")
    public Result<Void> archive(@PathVariable String sessionId) {
        taskService.archive(sessionId);
        return Result.success(null);
    }

    /** 删除。 */
    @DeleteMapping("/{sessionId}")
    public Result<Void> delete(@PathVariable String sessionId) {
        taskService.delete(sessionId);
        return Result.success(null);
    }

    /** 移动到项目。 */
    @PostMapping("/{sessionId}/move")
    public Result<Void> move(@PathVariable String sessionId, @RequestBody MoveRequest request) {
        taskService.move(sessionId, request.getProjectId());
        return Result.success(null);
    }
}
