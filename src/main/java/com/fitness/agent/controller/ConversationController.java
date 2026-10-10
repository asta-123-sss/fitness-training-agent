package com.fitness.agent.controller;

import com.fitness.agent.model.dto.ConversationDetailResponse;
import com.fitness.agent.model.dto.ConversationResponse;
import com.fitness.agent.model.dto.ConversationShareResponse;
import com.fitness.agent.model.dto.CreateConversationResponse;
import com.fitness.agent.model.dto.MoveRequest;
import com.fitness.agent.model.dto.PinRequest;
import com.fitness.agent.model.dto.RenameRequest;
import com.fitness.agent.model.dto.Result;
import com.fitness.agent.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对话（历史任务）接口。
 */
@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public Result<CreateConversationResponse> create() {
        return Result.success(CreateConversationResponse.builder()
                .conversationId(conversationService.create())
                .build());
    }

    @GetMapping("/list")
    public Result<List<ConversationResponse>> list() {
        return Result.success(conversationService.list());
    }

    @GetMapping("/{conversationId}")
    public Result<ConversationDetailResponse> detail(@PathVariable String conversationId) {
        return Result.success(conversationService.detail(conversationId));
    }

    @GetMapping("/{conversationId}/share")
    public Result<ConversationShareResponse> share(@PathVariable String conversationId) {
        return Result.success(conversationService.share(conversationId));
    }

    @PostMapping("/{conversationId}/pin")
    public Result<Void> pin(@PathVariable String conversationId, @RequestBody PinRequest request) {
        conversationService.pin(conversationId, request.isPinned());
        return Result.success(null);
    }

    @PostMapping("/{conversationId}/rename")
    public Result<Void> rename(@PathVariable String conversationId, @Valid @RequestBody RenameRequest request) {
        conversationService.rename(conversationId, request.getTitle());
        return Result.success(null);
    }

    @PostMapping("/{conversationId}/archive")
    public Result<Void> archive(@PathVariable String conversationId) {
        conversationService.archive(conversationId);
        return Result.success(null);
    }

    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@PathVariable String conversationId) {
        conversationService.delete(conversationId);
        return Result.success(null);
    }

    @PostMapping("/{conversationId}/move")
    public Result<Void> move(@PathVariable String conversationId, @RequestBody MoveRequest request) {
        conversationService.move(conversationId, request.getProjectId());
        return Result.success(null);
    }
}
