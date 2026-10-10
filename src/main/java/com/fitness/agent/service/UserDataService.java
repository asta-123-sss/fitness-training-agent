package com.fitness.agent.service;

import com.fitness.agent.model.DietRecord;
import com.fitness.agent.model.WorkoutLog;
import com.fitness.agent.repository.DietRecordRepository;
import com.fitness.agent.repository.UserProfileRepository;
import com.fitness.agent.repository.WorkoutLogRepository;
import com.fitness.agent.util.JsonUtil;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聚合用户档案 / 饮食 / 打卡数据，供 AI 对话上下文读取复用。
 */
@Service
public class UserDataService {

    private final UserProfileRepository profileRepository;
    private final DietRecordRepository dietRepository;
    private final WorkoutLogRepository workoutRepository;

    public UserDataService(UserProfileRepository profileRepository,
                           DietRecordRepository dietRepository,
                           WorkoutLogRepository workoutRepository) {
        this.profileRepository = profileRepository;
        this.dietRepository = dietRepository;
        this.workoutRepository = workoutRepository;
    }

    /** 已保存档案的关键字段 JSON（供意图解析复用），无则 null。 */
    public String profileContextJson(Long userId) {
        if (userId == null) {
            return null;
        }
        return profileRepository.findByUserId(userId).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gender", p.getGender());
            m.put("age", p.getAge());
            m.put("heightCm", p.getHeightCm());
            m.put("weightKg", p.getWeightKg());
            m.put("activityLevel", p.getActivityLevel());
            m.put("goal", p.getGoal());
            m.put("bmr", p.getBmr());
            m.put("tdee", p.getTdee());
            return JsonUtil.toJson(m);
        }).orElse(null);
    }

    /** 组装「近期饮食 + 训练」文字上下文（供规划/汇总参考），无数据返回 null。 */
    public String buildContext(Long userId) {
        if (userId == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        profileRepository.findByUserId(userId).ifPresent(p -> {
            sb.append("- 身体档案：身高 ").append(p.getHeightCm()).append("cm、体重 ").append(p.getWeightKg())
                    .append("kg、目标 ").append(p.getGoal() == null ? "未设置" : p.getGoal())
                    .append("、BMR ").append(p.getBmr()).append(" 千卡、TDEE ").append(p.getTdee()).append(" 千卡\n");
        });

        LocalDate today = LocalDate.now();
        List<DietRecord> diets = dietRepository.findByUserIdAndRecordDateOrderByCreatedAtAsc(userId, today);
        if (!diets.isEmpty()) {
            double cal = 0, protein = 0;
            for (DietRecord d : diets) {
                cal += d.getCalories() == null ? 0 : d.getCalories();
                protein += d.getProtein() == null ? 0 : d.getProtein();
            }
            sb.append("- 今日饮食：共 ").append(diets.size()).append(" 条，总热量 ")
                    .append(Math.round(cal)).append(" 千卡、蛋白质 ").append(Math.round(protein)).append(" 克\n");
        }

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        List<WorkoutLog> weekWorkouts = workoutRepository.findByUserIdAndWorkoutDateBetween(userId, weekStart, today);
        if (!weekWorkouts.isEmpty()) {
            sb.append("- 本周训练：").append(weekWorkouts.size()).append(" 次\n");
        }

        if (sb.length() == 0) {
            return null;
        }
        return "用户近期数据：\n" + sb;
    }
}
