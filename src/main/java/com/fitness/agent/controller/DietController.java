package com.fitness.agent.controller;

import com.fitness.agent.model.Food;
import com.fitness.agent.model.dto.DietRecordRequest;
import com.fitness.agent.model.dto.DietRecordResponse;
import com.fitness.agent.model.dto.DietSummaryResponse;
import com.fitness.agent.model.dto.Result;
import com.fitness.agent.service.DietService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食工具：食物搜索、饮食记录、每日汇总对比。
 */
@RestController
@RequestMapping("/api/diet")
public class DietController {

    private final DietService dietService;

    public DietController(DietService dietService) {
        this.dietService = dietService;
    }

    /** 按名称搜索食物库。 */
    @GetMapping("/food/search")
    public Result<List<Food>> searchFood(@RequestParam(required = false) String q) {
        return Result.success(dietService.searchFood(q));
    }

    /** 添加一条饮食记录。 */
    @PostMapping("/record")
    public Result<DietRecordResponse> add(@Valid @RequestBody DietRecordRequest request) {
        return Result.success(dietService.add(request));
    }

    /** 某一天的饮食记录列表。 */
    @GetMapping("/records")
    public Result<List<DietRecordResponse>> records(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(dietService.listRecords(date));
    }

    /** 某一天的饮食汇总（合计 + 与目标热量对比）。 */
    @GetMapping("/summary")
    public Result<DietSummaryResponse> summary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(dietService.summary(date));
    }

    /** 删除一条饮食记录。 */
    @DeleteMapping("/record/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dietService.delete(id);
        return Result.success(null);
    }
}
