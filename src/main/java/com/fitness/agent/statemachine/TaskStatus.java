package com.fitness.agent.statemachine;

/**
 * 任务状态枚举。轻量自研状态机（替代已进入维护模式的 Spring StateMachine）。
 */
public enum TaskStatus {

    /** 待解析。 */
    PENDING_PARSE("待解析"),

    /** 规划中。 */
    PLANNING("规划中"),

    /** 评审中。 */
    REVIEWING("评审中"),

    /** 回复中（日常聊天直接回复，不经过规划流水线）。 */
    REPLYING("回复中"),

    /** 已完成。 */
    COMPLETED("已完成"),

    /** 失败。 */
    FAILED("失败");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
