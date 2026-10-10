package com.fitness.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 个人健身训练规划 Agent 启动类。
 *
 * <p>核心链路：提交健身需求 → 意图解析 → 规划 → Critic 评审 → 汇总 Markdown 方案 → 生成分享二维码。</p>
 */
@EnableAsync
@SpringBootApplication
public class FitnessAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitnessAgentApplication.class, args);
    }
}
