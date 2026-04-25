package com.aifactory.dto;

public record AgentStatus(
        String name,
        String role,
        String status,
        String model,
        String duration,
        Integer progress
) {
}
