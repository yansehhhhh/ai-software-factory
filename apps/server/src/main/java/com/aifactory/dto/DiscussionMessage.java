package com.aifactory.dto;

public record DiscussionMessage(
        String role,
        String content
) {
    public static DiscussionMessage ai(String content) {
        return new DiscussionMessage("ai", content);
    }

    public static DiscussionMessage user(String content) {
        return new DiscussionMessage("user", content);
    }
}