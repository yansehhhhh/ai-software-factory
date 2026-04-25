package com.aifactory.dto;

import java.util.List;

public record ResultView(
        boolean available,
        boolean designAvailable,
        String projectUrl,
        String reportUrl,
        String zipUrl,
        String prdMarkdown,
        List<PageSpec> pageSpecs,
        List<ComponentSpec> componentSpecs,
        List<UserFlowSpec> userFlowSpecs,
        List<String> uiGuidelines
) {
}
