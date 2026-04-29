package com.aifactory.dto;

public record ArtifactView(
        String stage,
        String name,
        String type,
        String path,
        boolean downloadable
) {
}
