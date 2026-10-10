package com.fitness.agent.service;

import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.UserAccount;
import com.fitness.agent.model.dto.AuthResponse;
import com.fitness.agent.model.dto.LoginRequest;
import com.fitness.agent.model.dto.QrCreateResponse;
import com.fitness.agent.model.dto.QrStatusResponse;
import com.fitness.agent.model.dto.RegisterRequest;
import com.fitness.agent.model.dto.UserVO;
import com.fitness.agent.repository.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务：注册、登录、退出、扫码登录。
 */
@Service
public class AuthService {

    private static final int QR_EXPIRE_SECONDS = 5 * 60;

    private final UserAccountRepository repository;
    private final AuthSessionService sessionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountRepository repository, AuthSessionService sessionService) {
        this.repository = repository;
        this.sessionService = sessionService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByPhone(request.getPhone())) {
            throw new BusinessException("该手机号已注册，请直接登录");
        }
        UserAccount user = new UserAccount();
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(normalizeNickname(request));
        repository.save(user);
        return issue(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = repository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException("手机号或密码不正确"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("手机号或密码不正确");
        }
        return issue(user);
    }

    public void logout(String token) {
        if (token != null) {
            sessionService.removeToken(token);
        }
    }

    public UserVO me(Long userId) {
        return toVO(loadUser(userId));
    }

    public QrCreateResponse createQr() {
        return new QrCreateResponse(sessionService.createQr(), QR_EXPIRE_SECONDS);
    }

    public QrStatusResponse qrStatus(String qrId) {
        String raw = sessionService.qrRaw(qrId);
        if (raw == null) {
            return new QrStatusResponse("EXPIRED", null, null);
        }
        if ("PENDING".equals(raw)) {
            return new QrStatusResponse("PENDING", null, null);
        }
        // 已确认：原子认领并给电脑端签发登录 token
        Long userId = sessionService.claimQr(qrId);
        if (userId == null) {
            // 已被其它轮询领取，视为已消费
            return new QrStatusResponse("EXPIRED", null, null);
        }
        UserAccount user = loadUser(userId);
        return new QrStatusResponse("CONFIRMED", sessionService.createToken(user.getId()), toVO(user));
    }

    public void qrConfirm(String qrId, Long userId) {
        if (sessionService.qrRaw(qrId) == null) {
            throw new BusinessException("二维码已过期，请刷新后重试");
        }
        sessionService.confirmQr(qrId, userId);
    }

    private AuthResponse issue(UserAccount user) {
        return new AuthResponse(sessionService.createToken(user.getId()), toVO(user));
    }

    private UserAccount loadUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("未登录或登录已过期");
        }
        return repository.findById(userId)
                .orElseThrow(() -> new BusinessException("账号不存在或已被删除"));
    }

    private UserVO toVO(UserAccount user) {
        return new UserVO(user.getId(), user.getPhone(), user.getNickname());
    }

    private String normalizeNickname(RegisterRequest request) {
        String nickname = request.getNickname();
        if (nickname == null || nickname.isBlank()) {
            String phone = request.getPhone();
            return "用户" + phone.substring(phone.length() - 4);
        }
        return nickname.trim();
    }
}
