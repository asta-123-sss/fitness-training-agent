package com.fitness.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fitness.agent.config.AgentProperties;
import com.fitness.agent.model.Exercise;
import com.fitness.agent.model.UserProfile;
import com.fitness.agent.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健身动作知识库：启动时从本地 exercises.json 加载，提供基于关键词/器械/部位的词法检索，
 * 并在检索时硬性排除与用户伤病禁忌冲突、以及器械不可用的动作（DeepSeek 无 embedding 接口，
 * 故采用词法检索替代向量检索，不引入向量库）。
 */
@Slf4j
@Component
public class ExerciseKnowledgeBase {

    private final AgentProperties properties;
    private List<Exercise> exercises = new ArrayList<>();

    public ExerciseKnowledgeBase(AgentProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        String json = JsonUtil.readClasspath("data/exercises.json");
        exercises = JsonUtil.readValue(json, new TypeReference<List<Exercise>>() {
        });
        log.info("健身动作知识库加载完成，共 {} 个动作", exercises.size());
    }

    /**
     * 根据用户画像检索合适的动作：先硬性排除，再按相关性打分取 topK。
     */
    public List<Exercise> search(UserProfile profile) {
        List<String> injuries = profile.getInjuries() == null ? List.of() : profile.getInjuries();
        List<String> equipment = profile.getEquipment() == null ? List.of() : profile.getEquipment();
        List<String> targets = profile.getTargetMuscles() == null ? List.of() : profile.getTargetMuscles();

        List<Exercise> candidates = new ArrayList<>();
        for (Exercise ex : exercises) {
            // 1. 硬性排除：禁忌动作与用户伤病冲突
            if (hasConflict(ex.getContraindications(), injuries)) {
                continue;
            }
            // 2. 硬性排除：动作需要器械但用户没有（徒手动作始终可用）
            if (!ex.getEquipment().isEmpty() && !hasAny(ex.getEquipment(), equipment)) {
                continue;
            }
            candidates.add(ex);
        }

        // 3. 相关性打分排序
        Map<Exercise, Integer> scores = new HashMap<>();
        for (Exercise ex : candidates) {
            int score = 0;
            if (ex.getEquipment().isEmpty()) {
                score += 2; // 徒手动作优先
            }
            score += categoryRelevance(profile.getGoal(), ex.getCategory());
            if (matchesTarget(ex, targets)) {
                score += 10; // 重点部位动作优先
            }
            if (ex.getDifficulty() <= 2) {
                score += 1; // 新手友好
            }
            scores.put(ex, score);
        }

        List<Exercise> result = candidates.stream()
                .sorted(Comparator.comparingInt((Exercise e) -> scores.getOrDefault(e, 0)).reversed())
                .limit(properties.getRetrievalTopK())
                .toList();
        log.info("RAG 检索到 {} 个候选动作", result.size());
        return result;
    }

    private boolean hasConflict(List<String> contraindications, List<String> injuries) {
        for (String c : contraindications) {
            for (String i : injuries) {
                if (c != null && i != null && (c.contains(i) || i.contains(c))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAny(List<String> needed, List<String> available) {
        for (String n : needed) {
            for (String a : available) {
                if (n != null && a != null && (n.contains(a) || a.contains(n))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int categoryRelevance(String goal, String category) {
        if ("减脂".equals(goal)) {
            if ("全身心肺".equals(category) || "核心".equals(category)) {
                return 3;
            }
            if ("腿".equals(category)) {
                return 2;
            }
            return 1;
        }
        if ("增肌".equals(goal)) {
            if ("胸".equals(category) || "背".equals(category) || "腿".equals(category) || "肩".equals(category)) {
                return 3;
            }
            if ("核心".equals(category)) {
                return 2;
            }
            return 1;
        }
        return 2;
    }

    /** 判断动作是否命中用户想重点锻炼的部位。 */
    private boolean matchesTarget(Exercise ex, List<String> targets) {
        if (targets == null || targets.isEmpty()) { return false; }
        for (String t : targets) {
            String cat = targetCategory(t);
            if (cat != null && cat.equals(ex.getCategory())) {
                return true;
            }
        }
        return false;
    }

    /** 把用户描述的部位（如 "胸部"、"胸肌"）映射到动作库的 category。 */
    private String targetCategory(String target) {
        if (target == null) { return null; }
        if (target.contains("胸")) { return "胸"; }
        if (target.contains("背") || target.contains("划船") || target.contains("引体")) { return "背"; }
        if (target.contains("腿") || target.contains("臀") || target.contains("深蹲") || target.contains("硬拉")) { return "腿"; }
        if (target.contains("肩") || target.contains("三角肌") || target.contains("臂") || target.contains("肱")) { return "肩"; }
        if (target.contains("腹") || target.contains("核心") || target.contains("腰")) { return "核心"; }
        if (target.contains("心肺") || target.contains("有氧") || target.contains("跑步")) { return "全身心肺"; }
        return null;
    }
}
