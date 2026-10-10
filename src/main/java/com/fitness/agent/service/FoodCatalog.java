package com.fitness.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fitness.agent.model.Food;
import com.fitness.agent.util.JsonUtil;

import java.util.List;
import java.util.Optional;

/**
 * 内置常见食物库：静态加载 data/foods.json（每 100 克营养数据）。
 * 采用静态工具类（而非 Spring Bean），使 ServiceLoader 实例化的 Agent 工具也能直接调用。
 */
public final class FoodCatalog {

    private static final List<Food> FOODS = load();

    private FoodCatalog() {
    }

    private static List<Food> load() {
        String json = JsonUtil.readClasspath("data/foods.json");
        return JsonUtil.readValue(json, new TypeReference<List<Food>>() {
        });
    }

    /** 全部食物。 */
    public static List<Food> all() {
        return FOODS;
    }

    /** 按名称/分类子串匹配搜索（最多 20 条）。 */
    public static List<Food> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.trim().toLowerCase();
        return FOODS.stream()
                .filter(f -> f.getName().toLowerCase().contains(q)
                        || (f.getCategory() != null && f.getCategory().toLowerCase().contains(q)))
                .limit(20)
                .toList();
    }

    /** 按名称精确匹配。 */
    public static Optional<Food> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String n = name.trim();
        return FOODS.stream().filter(f -> f.getName().equals(n)).findFirst();
    }
}
