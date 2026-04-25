package com.aifactory.dto;

import java.util.List;

public record ComponentSpec(
        String name,
        String purpose,
        List<String> notes
) {
}
