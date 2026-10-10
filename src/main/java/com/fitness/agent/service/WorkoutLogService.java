package com.fitness.agent.service;

import com.fitness.agent.auth.UserContext;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.WorkoutLog;
import com.fitness.agent.model.dto.WorkoutLogRequest;
import com.fitness.agent.model.dto.WorkoutLogResponse;
import com.fitness.agent.model.dto.WorkoutStatsResponse;
import com.fitness.agent.repository.WorkoutLogRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 训练打卡日志服务：新增、列表、简单频次统计。
 */
@Service
public class WorkoutLogService {

    private final WorkoutLogRepository repository;

    public WorkoutLogService(WorkoutLogRepository repository) {
        this.repository = repository;
    }

    /** 新增一条训练记录。 */
    public WorkoutLogResponse add(WorkoutLogRequest request) {
        Long userId = requireUser();
        WorkoutLog log = new WorkoutLog();
        log.setUserId(userId);
        log.setWorkoutDate(request.getWorkoutDate() != null ? request.getWorkoutDate() : LocalDate.now());
        log.setBodyPart(request.getBodyPart());
        log.setExerciseName(request.getExerciseName());
        log.setWeightKg(request.getWeightKg());
        log.setSets(request.getSets());
        log.setReps(request.getReps());
        log.setFeeling(request.getFeeling());
        repository.save(log);
        return toResponse(log);
    }

    /** 近期训练记录（按日期倒序）。 */
    public List<WorkoutLogResponse> list() {
        Long userId = UserContext.get();
        if (userId == null) {
            return List.of();
        }
        return repository.findByUserIdOrderByWorkoutDateDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 统计：累计次数、本周次数、各部位次数。 */
    public WorkoutStatsResponse stats() {
        Long userId = UserContext.get();
        if (userId == null) {
            return WorkoutStatsResponse.builder().totalCount(0).weekCount(0).byBodyPart(Map.of()).build();
        }
        List<WorkoutLog> all = repository.findByUserIdOrderByWorkoutDateDesc(userId);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        long weekCount = repository.findByUserIdAndWorkoutDateBetween(userId, weekStart, today).size();

        Map<String, Integer> byBodyPart = new LinkedHashMap<>();
        for (WorkoutLog w : all) {
            String part = w.getBodyPart() == null ? "未分类" : w.getBodyPart();
            byBodyPart.merge(part, 1, (a, b) -> a + b);
        }

        return WorkoutStatsResponse.builder()
                .totalCount(all.size())
                .weekCount(weekCount)
                .byBodyPart(byBodyPart)
                .build();
    }

    private WorkoutLogResponse toResponse(WorkoutLog w) {
        return WorkoutLogResponse.builder()
                .id(w.getId())
                .workoutDate(w.getWorkoutDate())
                .bodyPart(w.getBodyPart())
                .exerciseName(w.getExerciseName())
                .weightKg(w.getWeightKg())
                .sets(w.getSets())
                .reps(w.getReps())
                .feeling(w.getFeeling())
                .createdAt(w.getCreatedAt())
                .build();
    }

    private Long requireUser() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }
}
