package com.aifactory.dto;

import java.util.List;

public record ArtifactSection(
        String title,
        List<String> bullets
) {
}
