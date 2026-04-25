package com.aifactory.llm;

import org.springframework.stereotype.Component;

@Component
public class ModelRouter {

    public String resolveModel(String taskType, String modelHint) {
        if (modelHint != null && !modelHint.isBlank()) {
            return modelHint;
        }

        return switch (taskType) {
            case "prd" -> "gpt-4.1";
            case "ui" -> "qwen-max";
            default -> "gpt-4.1";
        };
    }
}
