package com.aifactory.dto;

import java.util.List;

public record DiscussionMessage(
        String role,
        String content,
        List<String> options,
        String time
) {
    public DiscussionMessage {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public static DiscussionMessage ai(String content) {
        return new DiscussionMessage("ai", content, List.of(), null);
    }

    public static DiscussionMessage ai(String content, String time) {
        return new DiscussionMessage("ai", content, List.of(), time);
    }

    public static DiscussionMessage ai(String content, List<String> options) {
        return new DiscussionMessage("ai", content, options, null);
    }

    public static DiscussionMessage user(String content) {
        return new DiscussionMessage("user", content, List.of(), null);
    }
}