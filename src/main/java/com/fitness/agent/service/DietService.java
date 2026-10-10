package com.fitness.agent.service;

import com.fitness.agent.auth.UserContext;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.DietRecord;
import com.fitness.agent.model.Food;
import com.fitness.agent.model.dto.DietRecordRequest;
import com.fitness.agent.model.dto.DietRecordResponse;
import com.fitness.agent.model.dto.DietSummaryResponse;
import com.fitness.agent.repository.DietRecordRepository;
import com.fitness.agent.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 饮食工具服务：食物搜索、饮食记录、每日汇总对比。
 */
@Service
public class DietService {

    private final DietRecordRepository dietRepository;
    private final UserProfileRepository profileRepository;

    public DietService(DietRecordRepository dietRepository, UserProfileRepository profileRepository) {
        this.dietRepository = dietRepository;
        this.profileRepository = profileRepository;
    }

    /** 按名称搜索食物库。 */
    public List<Food> searchFood(String query) {
        return FoodCatalog.search(query);
    }

    /** 添加一条饮食记录（根据食物库计算营养并冗余落库）。 */
    public DietRecordResponse add(DietRecordRequest request) {
        Long userId = requireUser();
        Food food = resolveFood(request.getFoodName())
                .orElseThrow(() -> new BusinessException("未找到该食物，请从搜索列表中选择"));

        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(request.getRecordDate() != null ? request.getRecordDate() : LocalDate.now());
        record.setMealType(request.getMealType());
        record.setFoodName(food.getName());
        record.setWeightGrams(request.getWeightGrams());
        double factor = request.getWeightGrams() / 100.0;
        record.setCalories(round1(food.getCalories() * factor));
        record.setProtein(round1(food.getProtein() * factor));
        record.setCarbs(round1(food.getCarbs() * factor));
        record.setFat(round1(food.getFat() * factor));
        dietRepository.save(record);

        return toResponse(record);
    }

    /** 某一天的饮食记录列表。 */
    public List<DietRecordResponse> listRecords(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        Long userId = UserContext.get();
        if (userId == null) {
            return List.of();
        }
        return dietRepository.findByUserIdAndRecordDateOrderByCreatedAtAsc(userId, d).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 某一天的饮食汇总（合计 + 与目标热量对比）。 */
    public DietSummaryResponse summary(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        Long userId = UserContext.get();
        if (userId == null) {
            return DietSummaryResponse.builder().date(d)
                    .totalCalories(0.0).totalProtein(0.0).totalCarbs(0.0).totalFat(0.0)
                    .targetCalories(null).records(List.of()).build();
        }

        List<DietRecord> records = dietRepository.findByUserIdAndRecordDateOrderByCreatedAtAsc(userId, d);
        double calories = 0, protein = 0, carbs = 0, fat = 0;
        for (DietRecord r : records) {
            calories += r.getCalories() == null ? 0 : r.getCalories();
            protein += r.getProtein() == null ? 0 : r.getProtein();
            carbs += r.getCarbs() == null ? 0 : r.getCarbs();
            fat += r.getFat() == null ? 0 : r.getFat();
        }

        Integer target = profileRepository.findByUserId(userId)
                .map(p -> p.getTargetCalories() != null ? p.getTargetCalories()
                        : (p.getTdee() != null ? (int) Math.round(p.getTdee()) : null))
                .orElse(null);

        return DietSummaryResponse.builder()
                .date(d)
                .totalCalories(round1(calories))
                .totalProtein(round1(protein))
                .totalCarbs(round1(carbs))
                .totalFat(round1(fat))
                .targetCalories(target)
                .records(records.stream().map(this::toResponse).toList())
                .build();
    }

    /** 删除一条饮食记录。 */
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException("记录不存在");
        }
        Long userId = requireUser();
        DietRecord record = dietRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        if (!Objects.equals(userId, record.getUserId())) {
            throw new BusinessException("无权访问该记录");
        }
        dietRepository.delete(record);
    }

    private DietRecordResponse toResponse(DietRecord r) {
        return DietRecordResponse.builder()
                .id(r.getId())
                .recordDate(r.getRecordDate())
                .mealType(r.getMealType())
                .foodName(r.getFoodName())
                .weightGrams(r.getWeightGrams())
                .calories(r.getCalories())
                .protein(r.getProtein())
                .carbs(r.getCarbs())
                .fat(r.getFat())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private Optional<Food> resolveFood(String name) {
        Optional<Food> exact = FoodCatalog.findByName(name);
        if (exact.isPresent()) {
            return exact;
        }
        return FoodCatalog.search(name).stream().findFirst();
    }

    private Long requireUser() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
