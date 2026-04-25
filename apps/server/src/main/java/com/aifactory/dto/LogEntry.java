package com.aifactory.dto;

public record LogEntry(
        String time,
        String agent,
        String level,
        String message
) {
}
