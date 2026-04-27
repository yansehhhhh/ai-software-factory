package com.aifactory.dto;

import java.util.List;

public record DiscussionChatResult(
        String question,
        boolean isComplete,
        List<DiscussionMessage> history
) {}