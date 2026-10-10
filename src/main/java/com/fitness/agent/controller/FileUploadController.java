package com.fitness.agent.controller;

import com.fitness.agent.config.UploadProperties;
import com.fitness.agent.exception.BusinessException;
import com.fitness.agent.model.dto.Result;
import com.fitness.agent.model.dto.UploadResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传接口：把图片/文本附件落盘到本地目录，返回可访问的 /uploads/ 相对路径。
 * 仅允许图片与纯文本类文件，并控制单文件大小。
 */
@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> TEXT_TYPES = Set.of(
            "text/plain", "text/markdown", "text/csv", "application/json");

    private final Path uploadDir;
    private final long maxSize;

    public FileUploadController(UploadProperties properties) {
        this.maxSize = properties.getUploadMaxSize();
        try {
            this.uploadDir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录：" + properties.getUploadDir(), e);
        }
    }

    @PostMapping("/upload")
    public Result<UploadResponse> upload(@RequestParam MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!IMAGE_TYPES.contains(contentType) && !TEXT_TYPES.contains(contentType))) {
            throw new BusinessException("仅支持图片（jpg/png/webp/gif）或文本文件（txt/md/csv/json）");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + extensionOf(file.getOriginalFilename(), contentType);
        try {
            Path dest = Objects.requireNonNull(uploadDir.resolve(name));
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败，请稍后重试");
        }
        return Result.success(new UploadResponse(name, file.getOriginalFilename(), "/uploads/" + name, contentType, file.getSize()));
    }

    private String extensionOf(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.'));
            if (ext.length() <= 8) {
                return ext.toLowerCase();
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "text/markdown" -> ".md";
            case "text/csv" -> ".csv";
            case "application/json" -> ".json";
            default -> ".txt";
        };
    }
}
