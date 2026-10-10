package com.fitness.agent.controller;

import com.fitness.agent.model.dto.Result;
import com.fitness.agent.model.dto.WorkoutLogRequest;
import com.fitness.agent.model.dto.WorkoutLogResponse;
import com.fitness.agent.model.dto.WorkoutStatsResponse;
import com.fitness.agent.service.WorkoutLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 训练打卡日志：新增记录、列表、频次统计。
 */
@RestController
@RequestMapping("/api/workout")
public class WorkoutLogController {

    private final WorkoutLogService workoutLogService;

    public WorkoutLogController(WorkoutLogService workoutLogService) {
        this.workoutLogService = workoutLogService;
    }

    /** 新增一条训练记录。 */
    @PostMapping
    public Result<WorkoutLogResponse> add(@Valid @RequestBody WorkoutLogRequest request) {
        return Result.success(workoutLogService.add(request));
    }

    /** 近期训练记录。 */
    @GetMapping("/list")
    public Result<List<WorkoutLogResponse>> list() {
        return Result.success(workoutLogService.list());
    }

    /** 频次统计。 */
    @GetMapping("/stats")
    public Result<WorkoutStatsResponse> stats() {
        return Result.success(workoutLogService.stats());
    }
}
