package com.aifactory.dto;

import java.util.List;

public record DiscussionMessage(
        String role,
        String content,
        List<String> options
) {
    public DiscussionMessage {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public static DiscussionMessage ai(String content) {
        return new DiscussionMessage("ai", content, List.of());
    }

    public static DiscussionMessage ai(String content, List<String> options) {
        return new DiscussionMessage("ai", content, options);
    }

    public static DiscussionMessage user(String content) {
        return new DiscussionMessage("user", content, List.of());
    }
}