package com.fitness.agent.service;

import com.fitness.agent.config.AgentProperties;
import com.fitness.agent.model.CriticVerdict;
import com.fitness.agent.model.Exercise;
import com.fitness.agent.model.IntentType;
import com.fitness.agent.model.TaskEntity;
import com.fitness.agent.model.UserProfile;
import com.fitness.agent.repository.TaskRepository;
import com.fitness.agent.statemachine.TaskStateMachine;
import com.fitness.agent.statemachine.TaskStatus;
import com.fitness.agent.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 流水线编排器：在后台线程中串起「意图解析 → RAG 检索 → 规划 → 评审 → 汇总」。
 * 每一步都通过状态机迁移并落库，前端据此轮询进度。
 */
@Slf4j
@Component
public class AgentOrchestrator {

    private final TaskRepository repository;
    private final TaskStateMachine stateMachine;
    private final SessionCacheService cache;
    private final ChatAgent chatAgent;
    private final IntentExtractorAgent intentExtractor;
    private final ExerciseKnowledgeBase knowledgeBase;
    private final PlannerAgent planner;
    private final CriticAgent critic;
    private final SummarizerAgent summarizer;
    private final UserDataService userDataService;
    private final AgentProperties properties;

    public AgentOrchestrator(TaskRepository repository,
                             TaskStateMachine stateMachine,
                             SessionCacheService cache,
                             ChatAgent chatAgent,
                             IntentExtractorAgent intentExtractor,
                             ExerciseKnowledgeBase knowledgeBase,
                             PlannerAgent planner,
                             CriticAgent critic,
                             SummarizerAgent summarizer,
                             UserDataService userDataService,
                             AgentProperties properties) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.cache = cache;
        this.chatAgent = chatAgent;
        this.intentExtractor = intentExtractor;
        this.knowledgeBase = knowledgeBase;
        this.planner = planner;
        this.critic = critic;
        this.summarizer = summarizer;
        this.userDataService = userDataService;
        this.properties = properties;
    }

    @Async("taskExecutor")
    public void execute(String sessionId) {
        TaskEntity task = repository.findBySessionId(sessionId).orElse(null);
        if (task == null) {
            log.warn("任务不存在：{}", sessionId);
            return;
        }

        try {
            // 1. 意图分类：日常聊天直接回复，训练请求才走规划流水线
            IntentType intent = chatAgent.classify(task.getUserInput());
            task.setIntentType(intent.name());
            if (intent == IntentType.CHAT || intent == IntentType.QNA) {
                updateStatus(task, TaskStatus.REPLYING);
                String reply = intent == IntentType.QNA
                        ? chatAgent.qa(task.getUserInput())
                        : chatAgent.reply(task.getUserInput());
                task.setPlanMarkdown(reply);
                updateStatus(task, TaskStatus.COMPLETED);
                repository.save(task);
                cache.clear(sessionId);
                log.info("任务 {} 直接回复完成（{}）", sessionId, intent.name());
                return;
            }

            // 2. 意图解析（优先复用 Redis 缓存，减少重复 LLM 调用；优先采用已保存档案）
            String knownProfileJson = userDataService.profileContextJson(task.getUserId());
            String userContext = userDataService.buildContext(task.getUserId());
            UserProfile profile = resolveProfile(task, knownProfileJson);
            updateStatus(task, TaskStatus.PLANNING);

            // 3. RAG 检索动作 + Planner 规划（支持 Function Call）
            List<Exercise> exercises = knowledgeBase.search(profile);
            String planJson = planner.plan(profile, exercises, task.getUserInput(), userContext);
            cache.putPlan(sessionId, planJson);

            // 4. Critic 评审循环，不通过则回传意见重新规划（最多 maxCriticIterations 次）
            for (int i = 0; i < properties.getMaxCriticIterations(); i++) {
                updateStatus(task, TaskStatus.REVIEWING);
                CriticVerdict verdict = critic.review(profile, planJson);
                if (verdict.isPass() || i == properties.getMaxCriticIterations() - 1) {
                    // 通过评审，或已达最大迭代次数，接受当前计划
                    break;
                }
                log.info("第 {} 次评审未通过，重新规划", i + 1);
                updateStatus(task, TaskStatus.PLANNING); // REVIEWING -> PLANNING
                planJson = planner.replan(profile, exercises, planJson, verdict, task.getUserInput(), userContext);
                cache.putPlan(sessionId, planJson);
            }

            // 5. 汇总为 Markdown 方案
            String markdown = summarizer.summarize(profile, planJson, task.getUserInput(), userContext);
            task.setProfileJson(JsonUtil.toJson(profile));
            task.setPlanJson(planJson);
            task.setPlanMarkdown(markdown);
            updateStatus(task, TaskStatus.COMPLETED);
            repository.save(task);
            cache.clear(sessionId);
            log.info("任务 {} 执行完成", sessionId);

        } catch (Exception e) {
            log.error("任务 {} 执行失败", sessionId, e);
            task.setErrorMsg(e.getMessage());
            updateStatus(task, TaskStatus.FAILED);
            repository.save(task);
        }
    }

    private UserProfile resolveProfile(TaskEntity task, String knownProfileJson) {
        String cached = cache.getProfile(task.getSessionId());
        if (cached != null) {
            log.info("复用 Redis 缓存的 profile：{}", task.getSessionId());
            return JsonUtil.readValue(cached, UserProfile.class);
        }
        UserProfile profile = intentExtractor.extract(task.getUserInput(), knownProfileJson);
        task.setProfileJson(JsonUtil.toJson(profile));
        cache.putProfile(task.getSessionId(), JsonUtil.toJson(profile));
        return profile;
    }

    private void updateStatus(@NonNull TaskEntity task, TaskStatus target) {
        stateMachine.transition(task, target);
        repository.save(task);
        log.info("任务 {} 状态迁移 -> {}", task.getSessionId(), target.getLabel());
    }
}
