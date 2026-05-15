package com.aifactory.dto;

public record GeneratedProjectPreviewResponse(
        String status,
        String url,
        int port,
        String projectDir
) {
}
