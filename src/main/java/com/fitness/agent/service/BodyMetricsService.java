package com.fitness.agent.service;

import com.fitness.agent.auth.UserContext;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.UserProfileEntity;
import com.fitness.agent.model.dto.BodyMetricsRequest;
import com.fitness.agent.model.dto.BodyMetricsResponse;
import com.fitness.agent.model.dto.TargetNutrition;
import com.fitness.agent.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 身体指标计算与档案保存服务。
 */
@Service
public class BodyMetricsService {

    private final UserProfileRepository profileRepository;

    public BodyMetricsService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /** 纯计算，不落库。 */
    public BodyMetricsResponse calculate(BodyMetricsRequest request) {
        return buildResponse(request, null, null);
    }

    /** 读取当前用户已保存的档案（未登录或未保存返回 null）。 */
    public BodyMetricsResponse getProfile() {
        Long userId = UserContext.get();
        if (userId == null) {
            return null;
        }
        return profileRepository.findByUserId(userId).map(this::toResponse).orElse(null);
    }

    /** 保存（upsert）当前用户的档案，返回保存后的结果。 */
    public BodyMetricsResponse saveProfile(BodyMetricsRequest request) {
        Long userId = requireUser();

        double bmi = NutritionCalculator.bmi(request.getHeightCm(), request.getWeightKg());
        double bmr = NutritionCalculator.bmr(request.getGender(), request.getAge(), request.getHeightCm(), request.getWeightKg());
        double tdee = NutritionCalculator.tdee(bmr, request.getActivityLevel());
        String goal = isBlank(request.getGoal()) ? "维持" : request.getGoal();
        int targetCalories = NutritionCalculator.targetCalories(tdee, goal);

        UserProfileEntity entity = profileRepository.findByUserId(userId).orElseGet(UserProfileEntity::new);
        entity.setUserId(userId);
        entity.setGender(request.getGender());
        entity.setAge(request.getAge());
        entity.setHeightCm(request.getHeightCm());
        entity.setWeightKg(request.getWeightKg());
        entity.setActivityLevel(isBlank(request.getActivityLevel()) ? "轻度" : request.getActivityLevel());
        entity.setBmi(round2(bmi));
        entity.setBmr(round1(bmr));
        entity.setTdee(round1(tdee));
        entity.setGoal(goal);
        entity.setTargetCalories(targetCalories);
        profileRepository.save(entity);

        return buildResponse(request, goal, targetCalories);
    }

    private BodyMetricsResponse toResponse(UserProfileEntity e) {
        BodyMetricsRequest req = new BodyMetricsRequest();
        req.setGender(e.getGender());
        req.setAge(e.getAge());
        req.setHeightCm(e.getHeightCm());
        req.setWeightKg(e.getWeightKg());
        req.setActivityLevel(e.getActivityLevel());
        return buildResponse(req, e.getGoal(), e.getTargetCalories());
    }

    private BodyMetricsResponse buildResponse(BodyMetricsRequest request, String goal, Integer targetCalories) {
        double bmi = NutritionCalculator.bmi(request.getHeightCm(), request.getWeightKg());
        double bmr = NutritionCalculator.bmr(request.getGender(), request.getAge(), request.getHeightCm(), request.getWeightKg());
        double tdee = NutritionCalculator.tdee(bmr, request.getActivityLevel());

        List<TargetNutrition> targets = new ArrayList<>();
        for (String g : List.of("减脂", "维持", "增肌")) {
            int cal = NutritionCalculator.targetCalories(tdee, g);
            NutritionCalculator.Macros m = NutritionCalculator.macros(g, request.getWeightKg(), cal);
            targets.add(TargetNutrition.builder()
                    .goal(g).calories(cal)
                    .protein(m.protein()).carbs(m.carbs()).fat(m.fat())
                    .build());
        }

        return BodyMetricsResponse.builder()
                .gender(request.getGender())
                .age(request.getAge())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .activityLevel(request.getActivityLevel())
                .bmi(round2(bmi))
                .bmiCategory(NutritionCalculator.bmiCategory(bmi))
                .bmr((int) Math.round(bmr))
                .tdee((int) Math.round(tdee))
                .goal(goal)
                .targetCalories(targetCalories)
                .targets(targets)
                .build();
    }

    private Long requireUser() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
