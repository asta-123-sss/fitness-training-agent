package com.fitness.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件上传响应。
 */
@Data
@AllArgsConstructor
public class UploadResponse {

    /** 服务端存储文件名（不含路径）。 */
    private String name;

    /** 原始文件名。 */
    private String originalName;

    /** 对外可访问的相对 URL（/uploads/xxx）。 */
    private String url;

    /** 内容类型。 */
    private String contentType;

    /** 文件大小（字节）。 */
    private long size;
}
