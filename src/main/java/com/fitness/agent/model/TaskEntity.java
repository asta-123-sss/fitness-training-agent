package com.fitness.agent.model;

import com.fitness.agent.statemachine.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 健身规划任务持久化实体，保存用户输入、解析结果与最终训练方案。
 */
@Data
@Entity
@Table(name = "fitness_task")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户 ID（可空，兼容历史匿名任务）。 */
    private Long userId;

    /** 会话 ID，用于分享链接与轮询。 */
    @Column(nullable = false, unique = true, length = 64)
    private String sessionId;

    /** 所属对话 ID（可空，兼容历史单轮任务）；一条对话聚合同一页面的多轮问答。 */
    @Column(length = 64)
    private String conversationId;

    /** 用户原始输入。 */
    @Column(columnDefinition = "TEXT")
    private String userInput;

    /** 意图解析得到的结构化信息（JSON）。 */
    @Column(columnDefinition = "TEXT")
    private String profileJson;

    /** 评审通过的训练计划（JSON）。 */
    @Column(columnDefinition = "LONGTEXT")
    private String planJson;

    /** 最终 Markdown 训练方案。 */
    @Column(columnDefinition = "LONGTEXT")
    private String planMarkdown;

    /** 任务状态。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status;

    /** 意图类型：PLAN（训练规划）/ CHAT（日常聊天）；可空，历史数据视为 PLAN。 */
    @Column(length = 16)
    private String intentType;

    /** 失败原因。 */
    @Column(columnDefinition = "TEXT")
    private String errorMsg;

    /** 自定义标题（重命名后使用），为空时前端回退展示 userInput。 */
    private String title;

    /** 是否置顶。 */
    private Boolean pinned;

    /** 置顶时间（用于置顶项之间的排序）。 */
    private LocalDateTime pinnedAt;

    /** 是否归档（归档后从默认列表隐藏）。 */
    private Boolean archived;

    /** 所属项目 ID（可空，未分配项目为 null）。 */
    private Long projectId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
