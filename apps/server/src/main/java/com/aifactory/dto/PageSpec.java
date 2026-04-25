package com.aifactory.dto;

import java.util.List;

public record PageSpec(
        String name,
        String purpose,
        List<String> sections
) {
}
