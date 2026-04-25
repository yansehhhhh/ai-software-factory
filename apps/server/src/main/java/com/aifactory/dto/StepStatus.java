package com.aifactory.dto;

public record StepStatus(
        int index,
        String key,
        String title,
        String status,
        int progress,
        String detail,
        String duration,
        String error
) {
}
