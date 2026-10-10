package com.fitness.agent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话实体：一个「历史任务」即一个对话，聚合同一页面内多轮问答。
 * 置顶 / 重命名 / 归档 / 项目 等元数据都挂在对话上。
 */
@Data
@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户 ID。 */
    private Long userId;

    /** 对话公开标识，用于分享链接与前端定位。 */
    @Column(nullable = false, unique = true, length = 64)
    private String conversationId;

    /** 自定义标题（重命名后使用），为空时前端回退展示首条提问。 */
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
