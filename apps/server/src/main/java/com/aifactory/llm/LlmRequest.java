package com.aifactory.llm;

import java.util.Map;

public record LlmRequest(
        String taskType,
        String model,
        String prompt,
        String requirement,
        Map<String, Object> context
) {
}
