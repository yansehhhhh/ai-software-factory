package com.aifactory.dto;

import java.util.List;

public record UserFlowSpec(
        String name,
        List<String> steps
) {
}
