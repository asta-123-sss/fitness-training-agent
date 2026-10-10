package com.fitness.agent.repository;

import com.fitness.agent.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * 训练打卡日志仓库。
 */
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    List<WorkoutLog> findByUserIdOrderByWorkoutDateDesc(Long userId);

    List<WorkoutLog> findByUserIdAndWorkoutDateBetween(Long userId, LocalDate start, LocalDate end);
}
