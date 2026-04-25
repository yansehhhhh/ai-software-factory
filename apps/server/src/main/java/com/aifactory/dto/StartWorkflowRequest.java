package com.aifactory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartWorkflowRequest(
        @NotBlank(message = "需求内容不能为空")
        @Size(max = 2000, message = "需求内容不能超过 2000 个字符")
        String requirement
) {
}
