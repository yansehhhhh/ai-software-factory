package com.aifactory.dto;

public record ProjectSummary(
        String id,
        String name,
        String path,
        String updatedAt,
        boolean hasProductArtifacts,
        boolean hasUiArtifacts
) {
}
