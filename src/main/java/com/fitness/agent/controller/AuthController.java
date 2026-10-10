package com.fitness.agent.controller;

import com.fitness.agent.auth.AuthInterceptor;
import com.fitness.agent.auth.UserContext;
import com.fitness.agent.model.dto.AuthResponse;
import com.fitness.agent.model.dto.LoginRequest;
import com.fitness.agent.model.dto.QrConfirmRequest;
import com.fitness.agent.model.dto.QrCreateResponse;
import com.fitness.agent.model.dto.QrStatusResponse;
import com.fitness.agent.model.dto.RegisterRequest;
import com.fitness.agent.model.dto.Result;
import com.fitness.agent.model.dto.UserVO;
import com.fitness.agent.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 退出 / 扫码登录。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(resolveToken(authorization));
        return Result.success(null);
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.me(UserContext.get()));
    }

    @PostMapping("/qr/create")
    public Result<QrCreateResponse> createQr() {
        return Result.success(authService.createQr());
    }

    @GetMapping("/qr/status")
    public Result<QrStatusResponse> qrStatus(@RequestParam String qrId) {
        return Result.success(authService.qrStatus(qrId));
    }

    @PostMapping("/qr/confirm")
    public Result<Void> qrConfirm(@Valid @RequestBody QrConfirmRequest request) {
        authService.qrConfirm(request.getQrId(), UserContext.get());
        return Result.success(null);
    }

    private String resolveToken(String authorization) {
        if (authorization != null && authorization.startsWith(AuthInterceptor.BEARER_PREFIX)) {
            return authorization.substring(AuthInterceptor.BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
