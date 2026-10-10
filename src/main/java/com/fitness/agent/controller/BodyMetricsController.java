package com.fitness.agent.controller;

import com.fitness.agent.model.dto.BodyMetricsRequest;
import com.fitness.agent.model.dto.BodyMetricsResponse;
import com.fitness.agent.model.dto.Result;
import com.fitness.agent.service.BodyMetricsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 身体指标计算器：BMI / BMR / TDEE 与三档热量推荐、用户档案存取。
 */
@RestController
@RequestMapping("/api/body")
public class BodyMetricsController {

    private final BodyMetricsService bodyMetricsService;

    public BodyMetricsController(BodyMetricsService bodyMetricsService) {
        this.bodyMetricsService = bodyMetricsService;
    }

    /** 纯计算，不落库。 */
    @PostMapping("/calculate")
    public Result<BodyMetricsResponse> calculate(@Valid @RequestBody BodyMetricsRequest request) {
        return Result.success(bodyMetricsService.calculate(request));
    }

    /** 读取当前用户已保存档案（未登录或未保存返回 data=null）。 */
    @GetMapping("/profile")
    public Result<BodyMetricsResponse> getProfile() {
        return Result.success(bodyMetricsService.getProfile());
    }

    /** 保存（upsert）档案。 */
    @PostMapping("/profile")
    public Result<BodyMetricsResponse> saveProfile(@Valid @RequestBody BodyMetricsRequest request) {
        return Result.success(bodyMetricsService.saveProfile(request));
    }
}
